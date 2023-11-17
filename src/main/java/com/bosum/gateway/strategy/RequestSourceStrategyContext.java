package com.bosum.gateway.strategy;

import com.bosum.gateway.enums.RequestSourceEnum;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;


/**
 *  @author zuhao.ouyang
 */
public interface RequestSourceStrategyContext {
    Mono<Void> check(RequestSourceEnum type, ServerWebExchange exchange, GatewayFilterChain chain);
}
