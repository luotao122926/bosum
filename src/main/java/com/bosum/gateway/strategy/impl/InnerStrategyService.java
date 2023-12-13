package com.bosum.gateway.strategy.impl;

import com.bosum.gateway.enums.ProcessTypeEnumFlag;
import com.bosum.gateway.enums.RequestSourceEnum;
import com.bosum.gateway.strategy.Strategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 具体策略（ConcreteStrategy）：具体策略是实现策略接口的类。
 * 博商内部系统调用校验
 *
 * @author zuhao.ouyang
 */
@Slf4j
@Service
@ProcessTypeEnumFlag(RequestSourceEnum.INNER)
@RequiredArgsConstructor
public class InnerStrategyService implements Strategy {

    @Override
    public Mono<Void> check(ServerWebExchange exchange, GatewayFilterChain chain) {
        //要判断是否是新的erp2接口
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpRequest.Builder mutate = request.mutate();
        return chain.filter(exchange.mutate().request(mutate.build()).build());
    }

}
