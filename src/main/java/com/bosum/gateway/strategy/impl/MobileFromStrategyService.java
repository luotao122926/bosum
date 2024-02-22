package com.bosum.gateway.strategy.impl;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import com.bosum.framework.common.constants.TokenConstants;
import com.bosum.framework.common.util.jwt.JwtUtils;
import com.bosum.gateway.enums.ProcessTypeEnumFlag;
import com.bosum.gateway.enums.RequestSourceEnum;
import com.bosum.gateway.strategy.Strategy;
import com.bosum.gateway.util.RespUtils;
import com.bosum.gateway.util.WebFrameworkUtils;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static com.bosum.gateway.util.WebFrameworkUtils.removeHeader;

/**
 * Copyright (C), 2024-02-22
 * FileName: MobileFromStrategyService
 *
 * @author: zuhao.ouyang
 * Date:     2024/2/22 14:28
 * Description:
 */
@Slf4j
@Service
@ProcessTypeEnumFlag(RequestSourceEnum.MOBILE_FROM)
@RequiredArgsConstructor
public class MobileFromStrategyService  implements Strategy {

    private final RedisTemplate<String,Object> redisTemplate;

    public static final String mobile_login = "mobile_code:user:type:";

    @Override
    public Mono<Void> check(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpRequest.Builder mutate = request.mutate();
        String clientType = request.getHeaders().getFirst("Clienttype");
        String token = getToken(request);
        if (StrUtil.isEmpty(token)) {
            return RespUtils.unauthorizedResponse(exchange, "令牌不能为空");
        }
        Claims claims ;
        try {
            claims = JwtUtils.parseToken(token);
            if (claims == null) {
                return RespUtils.unauthorizedResponse(exchange, "令牌已过期或验证不正确！");
            }
        } catch (Exception e) {
            log.error("解析token错误", e);
            return RespUtils.unauthorizedResponse(exchange, "token不对，请重新登录");
        }
        String mobile = claims.get("mobile", String.class);
        boolean isLogin = Boolean.TRUE.equals(redisTemplate.hasKey(getTokenKey(mobile,clientType)));
        log.info("isLogin {}", isLogin);
        if (!isLogin) {
            return RespUtils.unauthorizedResponse(exchange, "登录状态已过期");
        }
        // 设置用户信息到请求
        WebFrameworkUtils.addHeader(mutate, "mobile", mobile);
        // 内部请求来源参数清除
        removeHeader(mutate);
        return chain.filter(exchange.mutate().request(mutate.build()).build());
    }

    /**
     * 获取请求token
     */
    private String getToken(ServerHttpRequest request) {
        String token = request.getHeaders().getFirst(TokenConstants.AUTHENTICATION);
        // 如果前端设置了令牌前缀，则裁剪掉前缀
        if (StrUtil.isNotEmpty(token) && token.startsWith(TokenConstants.PREFIX)) {
            token = token.replaceFirst(TokenConstants.PREFIX, StrUtil.EMPTY);
        }
        return token;
    }

    private String getTokenKey(String mobile, String clientType) {
        return mobile_login +mobile+ clientType;
    }
}
