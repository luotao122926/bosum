package com.bosum.gateway.util;

import cn.hutool.json.JSONUtil;
import com.bosum.framework.common.core.ResultData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;


@Slf4j
public class RespUtils {

    public static Mono<Void> unauthorizedResponse(ServerWebExchange exchange, String msg) {
        log.error("[鉴权异常处理]请求路径:{},错误信息为: {}", exchange.getRequest().getPath(),msg);
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.OK);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        ResultData<?> result = new ResultData<>(5005,msg);
        DataBuffer dataBuffer = response.bufferFactory().wrap(JSONUtil.toJsonPrettyStr(result).getBytes());
        return response.writeWith(Mono.just(dataBuffer));
    }

    public static Mono<Void> unauthorizedResponse(ServerWebExchange exchange, Integer code, String msg) {
        log.error("[鉴权异常处理]请求路径:{},错误信息为: {}", exchange.getRequest().getPath(),msg);
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.OK);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        ResultData<?> result = new ResultData<>(code,msg);
        DataBuffer dataBuffer = response.bufferFactory().wrap(JSONUtil.toJsonPrettyStr(result).getBytes());
        return response.writeWith(Mono.just(dataBuffer));
    }
}
