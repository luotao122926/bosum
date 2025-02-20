package com.bosum.gateway.strategy.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.bosum.framework.common.constants.SecurityConstants;
import com.bosum.gateway.enums.ProcessTypeEnumFlag;
import com.bosum.gateway.enums.RequestSourceEnum;
import com.bosum.gateway.service.InnerUserLoginService;
import com.bosum.gateway.strategy.Strategy;
import com.bosum.gateway.util.RespUtils;
import com.bosum.gateway.util.WebFrameworkUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 具体策略（ConcreteStrategy）：具体策略是实现策略接口的类。
 * 博商内部系统调用校验
 *
 * @author zuhao.ouyang
 */
@Slf4j
@Service
@ProcessTypeEnumFlag(RequestSourceEnum.INNER)
public class InnerStrategyService implements Strategy {

    @Autowired
    private InnerUserLoginService userLoginService;

    @Autowired
    private RedisTemplate<String,Object> redisTemplate;

    @Override
    public Mono<Void> check(ServerWebExchange exchange, GatewayFilterChain chain) {
        //要判断是否是新的erp2接口
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpRequest.Builder mutate = request.mutate();
        String bosumforid = request.getHeaders().getFirst("Bosumforid");
        log.info("inner invoke old to new bosumforid: {}" , bosumforid);
        if (StrUtil.isEmpty(bosumforid)) {
            return RespUtils.unauthorizedResponse(exchange, "系统内部错误");
        }
        // 判断redis是否有改key
        boolean flag = Boolean.TRUE.equals(redisTemplate.hasKey(bosumforid));

        if (!flag) {
            return RespUtils.unauthorizedResponse(exchange, "系统内部错误");
        }
        String userId = (String) redisTemplate.opsForValue().get(bosumforid);
        log.info("inner invoke old to new userId: {}" , userId);
        if (StrUtil.isNotEmpty(userId)) {
            try {
                JSONObject userInfo = userLoginService.login(userId);
                if(ObjectUtil.isEmpty(userInfo)){
                    return RespUtils.unauthorizedResponse(exchange, "用户不存在");
                }

                List<String> headerKeyArr = SecurityConstants.getHeaderKey();
                if(!CollUtil.isEmpty(headerKeyArr)){
                    for (String headerKey : headerKeyArr) {
                        WebFrameworkUtils.addHeader(mutate, userInfo, headerKey);
                    }
                }

            } catch (Exception e) {
                log.error("调用接口失败 ", e);
                return RespUtils.unauthorizedResponse(exchange, "用户授权登录失败");
            }
        }
        return chain.filter(exchange.mutate().request(mutate.build()).build());
    }



}
