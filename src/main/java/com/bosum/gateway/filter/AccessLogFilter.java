package com.bosum.gateway.filter;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.http.useragent.Platform;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import com.bosum.framework.common.constants.SecurityConstants;
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
public class AccessLogFilter implements GlobalFilter, Ordered {

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        addPlatform(exchange.getRequest());
        return chain.filter(exchange);
    }

    private void addPlatform(ServerHttpRequest request){
        String userAgentStr = request.getHeaders().getFirst("User-Agent");
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

        WebFrameworkUtils.addHeader(request.mutate(), SecurityConstants.HEADER_USER_AGENT, clientType.name());
    }

}
