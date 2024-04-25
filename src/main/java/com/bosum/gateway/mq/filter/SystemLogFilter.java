package com.bosum.gateway.mq.filter;


import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.text.StrPool;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.bosum.framework.common.util.date.DateTimeUtil;
import com.bosum.framework.common.util.jwt.JwtUtils;
import com.bosum.framework.common.util.validation.ValidationUtil;
import com.bosum.gateway.mq.config.properties.SystemLogProperties;
import com.bosum.gateway.mq.constant.Topic;
import com.bosum.gateway.mq.entity.SystemRequestLog;
import com.bosum.gateway.mq.kafka.KafkaSender;
import com.bosum.gateway.mq.utils.DateUtils;
import eu.bitwalker.useragentutils.UserAgent;
import io.jsonwebtoken.Claims;
import io.netty.util.internal.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.factory.rewrite.CachedBodyOutputMessage;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.BodyInserterContext;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.multipart.FormFieldPart;
import org.springframework.http.codec.multipart.Part;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;


/**
 * hhl
 * @return
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SystemLogFilter implements GlobalFilter, Ordered {

    private final KafkaSender kafkaSender;

    private final SystemLogProperties systemLogProperties;

    private final List<HttpMessageReader<?>> messageReaders = HandlerStrategies.withDefaults().messageReaders();

    /**
     * 顺序必须是<-1，否则标准的NettyWriteResponseFilter将在您的过滤器得到一个被调用的机会之前发送响应
     * 也就是说如果不小于 -1 ，将不会执行获取后端响应的逻辑
     * @return
     */
    @Override
    public int getOrder() {
        return -100;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        //判断是否打开相应是日志配置
        if (!systemLogProperties.getEnabled()){
            return chain.filter(exchange);
        }
        ServerHttpRequest request = exchange.getRequest().mutate()
                 //将获取的真实ip存入header微服务方便获取
                .header("X-Real-IP",exchange.getRequest().getRemoteAddress().getHostString())
                .build();
        String requestPath = request.getPath().pathWithinApplication().value();  // 请求路径
        Route route = getGatewayRoute(exchange);
        SystemRequestLog systemRequestLog=new SystemRequestLog();
        systemRequestLog.setProjectCloud("erp");
        systemRequestLog.setLogId(UUID.randomUUID().toString());
        systemRequestLog.setSchema(request.getURI().getScheme());
        systemRequestLog.setRequestMethod(request.getMethodValue());
        systemRequestLog.setRequestPath(requestPath);
        systemRequestLog.setTargetServer(route.getId());
        systemRequestLog.setRequestTime(new Date());
        systemRequestLog.setCreationTime(DateUtils.getDateString(new Date()));
        systemRequestLog.setMethod(request.getMethodValue());
        String clienttype = request.getHeaders().getFirst("clienttype");
        if(!ValidationUtil.isEmpty(clienttype)){
            systemRequestLog.setRequestSourceType(clienttype);
        }
        if(requestPath.contains(String.valueOf("/"))){
            String[] parts = requestPath.split("/");
            String result = parts[parts.length-1];
            systemRequestLog.setMethod(result);
        }else{
            systemRequestLog.setMethod(requestPath);
        }
        String userAgentHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.USER_AGENT);
        if(!ValidationUtil.isEmpty(userAgentHeader)){
            systemRequestLog.setEquipmentName(userAgentHeader);
            UserAgent userAgent = UserAgent.parseUserAgentString(userAgentHeader);
            String os = userAgent.getOperatingSystem().getName();
            systemRequestLog.setOperatingSystem(os);
        }

        String ip = request.getHeaders().getFirst("X-Real-IP"); //获取访问ip
        String ip2 = request.getHeaders().getFirst("X-Forwarded-For");//获取nginx代理真实访问ip
        if(null==ip2){
            systemRequestLog.setIp("0:0:0:0:0:0:0:1".equals(ip) ? "127.0.0.1" : ip);
        }else{
            systemRequestLog.setIp(ip2+"/"+ip);
        }

        MediaType mediaType = request.getHeaders().getContentType();
        if (!Objects.isNull(mediaType)){
            systemRequestLog.setRequestContentType(mediaType.getType() + "/" + mediaType.getSubtype());
        }
        //对不同的请求类型做相应的处理
        if(MediaType.APPLICATION_FORM_URLENCODED.isCompatibleWith(mediaType) || MediaType.APPLICATION_JSON.isCompatibleWith(mediaType)){
            return writeBodyLog(exchange, chain, systemRequestLog);
        }/*else if (MediaType.MULTIPART_FORM_DATA.isCompatibleWith(mediaType)){
            return readFormData(exchange,chain,systemRequestLog);
        }*/else{
            return writeBasicLog(exchange, chain, systemRequestLog);
        }
    }

    private Mono<Void> writeBasicLog(ServerWebExchange exchange, GatewayFilterChain chain, SystemRequestLog accessLog) {
        StringBuilder builder = new StringBuilder();
        MultiValueMap<String, String> queryParams = exchange.getRequest().getQueryParams();
        for (Map.Entry<String, List<String>> entry : queryParams.entrySet()) {
            builder.append(entry.getKey()).append("=").append(entry.getValue()).append(StrPool.COMMA);
        }
        accessLog.setRequestBody(builder.toString());
        //获取响应体
        ServerHttpResponseDecorator decoratedResponse = recordResponseLog(exchange, accessLog);

        return chain.filter(exchange.mutate().response(decoratedResponse).build())
                .then(Mono.fromRunnable(() -> {
                    // 打印日志
                    writeAccessLog(accessLog,exchange);
                }));
    }

    /**
     * 解决 request body 只能读取一次问题，
     * 参考: org.springframework.cloud.gateway.filter.factory.rewrite.ModifyRequestBodyGatewayFilterFactory
     * @param exchange
     * @param chain
     * @param gatewayLog
     * @return
     */
    @SuppressWarnings("unchecked")
    private Mono writeBodyLog(ServerWebExchange exchange, GatewayFilterChain chain, SystemRequestLog gatewayLog) {
        ServerRequest serverRequest = ServerRequest.create(exchange,messageReaders);

        Mono<String> modifiedBody = serverRequest.bodyToMono(String.class)
                .flatMap(body ->{
                    gatewayLog.setRequestBody(body);
                    return Mono.just(body);
                });
        // 通过 BodyInserter 插入 body(支持修改body), 避免 request body 只能获取一次
        BodyInserter bodyInserter = BodyInserters.fromPublisher(modifiedBody, String.class);
        HttpHeaders headers = new HttpHeaders();
        headers.putAll(exchange.getRequest().getHeaders());
        // the new content type will be computed by bodyInserter  and then set in the request decorator
        headers.remove(HttpHeaders.CONTENT_LENGTH);

        CachedBodyOutputMessage outputMessage = new CachedBodyOutputMessage(exchange, headers);

        return bodyInserter.insert(outputMessage,new BodyInserterContext())
                .then(Mono.defer(() -> {
                    // 重新封装请求
                    ServerHttpRequest decoratedRequest = requestDecorate(exchange, headers, outputMessage);
                    // 记录响应日志
                    ServerHttpResponseDecorator decoratedResponse = recordResponseLog(exchange, gatewayLog);
                    // 记录普通的
                    return chain.filter(exchange.mutate().request(decoratedRequest).response(decoratedResponse).build())
                            .then(Mono.fromRunnable(() -> {
                                // 打印日志
                                writeAccessLog(gatewayLog,exchange);
                            }));
                }));
    }

    /**
     * 打印日志 存储日志
     */
    private void writeAccessLog(SystemRequestLog gatewayLog,ServerWebExchange exchange) {
        log.info("打印日志"+gatewayLog.toString());
        ServerHttpRequest request = exchange.getRequest();
        //获取用户信息
        String token = request.getHeaders().getFirst("token");
        if(!ValidationUtil.isEmpty(token)) {
            Claims claims = JwtUtils.parseToken(token);
            String userName = JwtUtils.getUserName(claims);
            String userCode = JwtUtils.getUserCode(claims);
            gatewayLog.setRequestName(userName);
            gatewayLog.setUserCode(userCode);
        }
        //异步存储消息
        kafkaSender.sendMessage(Topic.topic_system_log, JSONUtil.toJsonStr(gatewayLog));
    }

    /**
     * 获得当前请求分发的路由
     * @param exchange
     * @return
     */
    private Route getGatewayRoute(ServerWebExchange exchange) {
        return exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
    }


    /**
     * 请求装饰器，重新计算 headers
     * @param exchange
     * @param headers
     * @param outputMessage
     * @return
     */
    private ServerHttpRequestDecorator requestDecorate(ServerWebExchange exchange, HttpHeaders headers,
                                                       CachedBodyOutputMessage outputMessage) {
        return new ServerHttpRequestDecorator(exchange.getRequest()) {
            @Override
            public HttpHeaders getHeaders() {
                long contentLength = headers.getContentLength();
                HttpHeaders httpHeaders = new HttpHeaders();
                httpHeaders.putAll(super.getHeaders());
                if (contentLength > 0) {
                    httpHeaders.setContentLength(contentLength);
                } else {
                    // TODO: this causes a 'HTTP/1.1 411 Length Required' // on
                    httpHeaders.set(HttpHeaders.TRANSFER_ENCODING, "chunked");
                }
                return httpHeaders;
            }

            @Override
            public Flux<DataBuffer> getBody() {
                return outputMessage.getBody();
            }
        };
    }


    /**
     * 记录响应日志
     * 通过 DataBufferFactory 解决响应体分段传输问题。
     */
    private ServerHttpResponseDecorator recordResponseLog(ServerWebExchange exchange, SystemRequestLog gatewayLog) {
        ServerHttpResponse response = exchange.getResponse();
        DataBufferFactory bufferFactory = response.bufferFactory();

        return new ServerHttpResponseDecorator(response) {
            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                if (body instanceof Flux) {
                    Date responseTime =new Date();
                    gatewayLog.setResponseTime(responseTime);
                    // 计算执行时间
                    long executeTime = (responseTime.getTime() - gatewayLog.getRequestTime().getTime());
                    // 设置计算执行时间
                    gatewayLog.setExecuteTime(executeTime);
                    //设置状态
                    gatewayLog.setCode(this.getStatusCode().value());
                    // 获取响应类型，如果是 json 就打印
                    String originalResponseContentType = exchange.getAttribute(ServerWebExchangeUtils.ORIGINAL_RESPONSE_CONTENT_TYPE_ATTR);

                    if (ObjectUtil.equal(this.getStatusCode(), HttpStatus.OK)
                            && !StringUtil.isNullOrEmpty(originalResponseContentType)
                            && originalResponseContentType.contains("application/json")) {

                        Flux<? extends DataBuffer> fluxBody = Flux.from(body);
                        return super.writeWith(fluxBody.buffer().map(dataBuffers -> {
                            // 合并多个流集合，解决返回体分段传输
                            DataBufferFactory dataBufferFactory = new DefaultDataBufferFactory();
                            DataBuffer join = dataBufferFactory.join(dataBuffers);
                            byte[] content = new byte[join.readableByteCount()];
                            join.read(content);
                            // 释放掉内存
                            DataBufferUtils.release(join);
                            String responseResult = new String(content, StandardCharsets.UTF_8);

                            gatewayLog.setResponseData(responseResult);

                            return bufferFactory.wrap(content);
                        }));
                    }
                }
                // if body is not a flux. never got there.
                return super.writeWith(body);
            }
        };
    }


    /**
     * 读取form-data数据
     * @param exchange
     * @param chain
     * @param gatewayLog
     * @return
     */
    private Mono<Void> readFormData(ServerWebExchange exchange, GatewayFilterChain chain, SystemRequestLog gatewayLog) {
        return DataBufferUtils.join(exchange.getRequest().getBody()).flatMap(dataBuffer -> {
            DataBufferUtils.retain(dataBuffer);
            final Flux<DataBuffer> cachedFlux = Flux.defer(() -> Flux.just(dataBuffer.slice(0, dataBuffer.readableByteCount())));
            final ServerHttpRequest mutatedRequest = new ServerHttpRequestDecorator(exchange.getRequest()) {
                @Override
                public Flux<DataBuffer> getBody() {
                    return cachedFlux;
                }
                @Override
                public MultiValueMap<String, String> getQueryParams() {
                    return UriComponentsBuilder.fromUri(exchange.getRequest().getURI()).build().getQueryParams();
                }
            };
            final HttpHeaders headers = exchange.getRequest().getHeaders();
            if (headers.getContentLength() == 0) {
                return chain.filter(exchange);
            }
            ResolvableType resolvableType;
            if (MediaType.MULTIPART_FORM_DATA.isCompatibleWith(headers.getContentType())) {
                resolvableType = ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, Part.class);
            } else {
                //解析 application/x-www-form-urlencoded
                resolvableType = ResolvableType.forClass(String.class);
            }
            return messageReaders.stream().filter(reader -> reader.canRead(resolvableType, mutatedRequest.getHeaders().getContentType())).findFirst().orElseThrow(() -> new IllegalStateException("no suitable HttpMessageReader.")).readMono(resolvableType, mutatedRequest, Collections.emptyMap()).flatMap(resolvedBody -> {
                if (resolvedBody instanceof MultiValueMap) {
                    LinkedMultiValueMap map = (LinkedMultiValueMap) resolvedBody;
                    if (CollectionUtil.isNotEmpty(map)) {
                        StringBuilder builder = new StringBuilder();
                        final Part bodyPartInfo = (Part) ((MultiValueMap) resolvedBody).getFirst("body");
                        if (bodyPartInfo instanceof FormFieldPart) {
                            String body = ((FormFieldPart) bodyPartInfo).value();
                            builder.append("body=").append(body);
                        }
                        gatewayLog.setRequestBody(builder.toString());
                    }
                } else {
                    gatewayLog.setRequestBody((String) resolvedBody);
                }
                //获取响应体
                ServerHttpResponseDecorator decoratedResponse = recordResponseLog(exchange, gatewayLog);
                return chain.filter(exchange.mutate().request(mutatedRequest).response(decoratedResponse).build()).then(Mono.fromRunnable(() -> {                                    // 打印日志
                    // 打印响应的日志
                    writeAccessLog(gatewayLog,exchange);
                }));
            });
        });
    }

}
