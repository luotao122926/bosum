package com.bosum.gateway.filter;


import cn.hutool.core.util.StrUtil;
import com.bosum.gateway.config.properties.CommonWhitesProperties;
import com.bosum.gateway.config.properties.ExposeWhitesProperties;
import com.bosum.gateway.config.properties.RequestThirdWhitesProperties;
import com.bosum.gateway.enums.RequestSourceEnum;
import com.bosum.gateway.strategy.RequestSourceStrategyContext;
import com.bosum.gateway.util.UrlUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Slf4j
@RequiredArgsConstructor
public class RequestSourceFilter implements GlobalFilter, Ordered {

    private final RequestSourceStrategyContext requestMethodStrategyContext;

    private final CommonWhitesProperties commonWhitesProperties;

    private final ExposeWhitesProperties exposeWhitesProperties;

    private final RequestThirdWhitesProperties requestThirdWhitesProperties;
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        // 首先各种策略模式是否满足条件
        String requestSource = request.getHeaders().getFirst("Requestsource");
        // 新系统标识
        String identifying = request.getHeaders().getFirst("Identifying");

        RequestSourceEnum requestSourceEnum = null;
        String url = request.getURI().getPath();
        log.info("请求url为: {}", url);
        // 顶层白名单，一律放行
        if (UrlUtils.matches(url, commonWhitesProperties.getUrl())) {
            return chain.filter(exchange);
        }
        // 对第三方暴露的接口，一律放行
        if (UrlUtils.matches(url, exposeWhitesProperties.getWhites())) {
            return chain.filter(exchange);
        }
        // erp请求第三方服务的时候，一律放行
        if (UrlUtils.matches(url, requestThirdWhitesProperties.getUrl())) {
            return chain.filter(exchange);
        }
        // 旧系统直接放行
        if (StrUtil.isEmpty(requestSource)) {
            return chain.filter(exchange);
        }
        if(RequestSourceEnum.NEW_ERP.getType().equals(identifying)){
            requestSourceEnum = RequestSourceEnum.NEW_ERP;
        }
        if (RequestSourceEnum.INNER.getType().equals(requestSource)) {
            requestSourceEnum = RequestSourceEnum.INNER;
        }
        return requestMethodStrategyContext.check(requestSourceEnum, exchange, chain);
    }

    @Override
    public int getOrder() {
        return 100;
    }
}
