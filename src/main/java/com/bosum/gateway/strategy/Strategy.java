package com.bosum.gateway.strategy;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;


/**
 *  @author zuhao.ouyang
 */
public interface Strategy {

    Mono<Void> check(ServerWebExchange exchange, GatewayFilterChain chain);
}
