package com.bosum.gateway.filter;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.http.useragent.Platform;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import com.bosum.framework.common.constants.SecurityConstants;
import com.bosum.framework.common.util.MDCTraceUtil;
import com.bosum.gateway.enums.ClientTypeEnum;
import com.bosum.gateway.util.WebFrameworkUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class TraceLogFilter implements GlobalFilter, Ordered {

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        addPlatform(exchange.getRequest());
       try{
           String traceId = exchange.getRequest().getHeaders().getFirst(MDCTraceUtil.TRACE_ID_HEADER);
           MDCTraceUtil.putIfAbsent(traceId);
           ServerHttpRequest.Builder mutate = exchange.getRequest().mutate();
           // 向下传递
           mutate.header(MDCTraceUtil.TRACE_ID_HEADER, MDCTraceUtil.getTraceId());
           return chain.filter(exchange.mutate().request(mutate.build()).build())
                   .then(Mono.fromRunnable(MDCTraceUtil::removeTrace));
       }catch (Exception e){
           return Mono.error(e);
       }
    }

    private void addPlatform(ServerHttpRequest request){
        String userAgentStr = request.getHeaders().getFirst("User-Agent");

        if(ObjUtil.isEmpty(userAgentStr)){
            return;
        }

        UserAgent userAgent = UserAgentUtil.parse(userAgentStr);
        ClientTypeEnum clientType = ClientTypeEnum.WEB;

        Platform platform = userAgent.getPlatform();
        if(ObjUtil.isNotEmpty(platform)){
            if(platform.isAndroid()){
                clientType = ClientTypeEnum.ANDROID;
            }

            if(platform.isIos()){
                clientType = ClientTypeEnum.IOS;
            }
        }

        WebFrameworkUtils.addHeader(request.mutate(), SecurityConstants.HEADER_USER_AGENT_PLATFORM, clientType.name());
    }
}
