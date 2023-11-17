package com.bosum.gateway.filter;

import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.reactive.CorsUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * 跨域 Filter
 *
 * @author ouyangzuhao
 */
@Component
public class CorsFilter implements WebFilter, Ordered {

    private static final String MAX_AGE = "3600L";
    private static final String METHODS ="POST, GET, PUT, OPTIONS, DELETE";
    private static final String HEADERS ="Origin, Cache-Control, No-Cache, Pragma, Access-Control-Max-Age,X-Requested-With, X_Requested_With, Content-Type, Content-Disposition, Content-Length, Date, Accept";

    @Override
    public int getOrder() {
        return -100;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // 非跨域请求，直接放行
        ServerHttpRequest request = exchange.getRequest();
        if (!CorsUtils.isCorsRequest(request)) {
            return chain.filter(exchange);
        }
        // 设置跨域响应头
        ServerHttpResponse response = exchange.getResponse();
        HttpHeaders headers = response.getHeaders();
        headers.add("Access-Control-Allow-Origin", request.getHeaders().getOrigin());
        headers.add("Access-Control-Allow-Credentials", "true");
        headers.add("Access-Control-Allow-Methods", METHODS);
        headers.add("Access-Control-Allow-Headers", HEADERS);
        headers.add("Access-Control-Max-Age", MAX_AGE);
        if (request.getMethod() == HttpMethod.OPTIONS) {
            response.setStatusCode(HttpStatus.OK);
            return Mono.empty();
        }
        return chain.filter(exchange);
    }
}
