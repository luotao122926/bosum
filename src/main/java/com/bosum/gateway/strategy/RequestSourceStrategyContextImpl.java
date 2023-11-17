package com.bosum.gateway.strategy;


import com.bosum.gateway.enums.RequestSourceEnum;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Optional;


/**
 *  @author zuhao.ouyang
 */
@Service
public class RequestSourceStrategyContextImpl implements RequestSourceStrategyContext {
    @Override
    public Mono<Void> check(RequestSourceEnum type, ServerWebExchange exchange, GatewayFilterChain chain) {
       return Optional.ofNullable(ProcessRequestSourceTypeConfig.generatorStrategyMap.get(type))
                .orElseThrow(() -> new RuntimeException("没有找到实现的bean" + this))
                .check(exchange,chain);
    }
}
