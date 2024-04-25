package com.bosum.gateway.strategy.impl;


import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.bosum.framework.common.constants.SecurityConstants;
import com.bosum.framework.common.constants.TokenConstants;
import com.bosum.framework.common.util.jwt.JwtUtils;
import com.bosum.gateway.constant.CacheConstants;
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

/**
 * 具体策略（ConcreteStrategy）：具体策略是实现策略接口的类。
 *  @author zuhao.ouyang
 */
@Slf4j
@Service
@ProcessTypeEnumFlag(RequestSourceEnum.NEW_ERP)//自定义注解，标注该类为FOCUS_PROCESS
@RequiredArgsConstructor
public class NewErpStrategyService implements Strategy {

    private final RedisTemplate<String,Object> redisTemplate;

    @Override
    public Mono<Void> check(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpRequest.Builder mutate = request.mutate();
        String clientType = request.getHeaders().getFirst("Clienttype");
        String token = getToken(request);
        log.info("请求头的信息的token {}", token);
        log.info("请求客户端clientType: {}", clientType);
        log.info("请求头信息: {}", JSONUtil.toJsonStr(request.getHeaders()));
        if (StrUtil.isEmpty(token)) {
            return RespUtils.unauthorizedResponse(exchange,"登录状态已过期");
        }
        Claims claims ;
        try {
             claims = JwtUtils.parseToken(token);
        } catch (Exception e) {
            log.error("解析token错误", e);
            return RespUtils.unauthorizedResponse(exchange,"登录状态已过期");
        }
        if (claims == null) {
            return RespUtils.unauthorizedResponse(exchange,"登录状态已过期");
        }
        String userid = JwtUtils.getUserId(claims);
        log.info("从redis获取token的信息key {}", getTokenKey(userid,clientType));
        Object redisToken = redisTemplate.opsForValue().get(getTokenKey(userid, clientType));
        if (ObjectUtil.isEmpty(redisToken)) {
            return RespUtils.unauthorizedResponse(exchange,"登录状态已过期");
        }
        String tokenStr = (String) redisToken;
        if (StrUtil.isEmpty(tokenStr)) {
            return RespUtils.unauthorizedResponse(exchange,"登录状态已过期");
        }
        // 作对比  用作踢人下线
        if (!tokenStr.equals(token)) {
            return RespUtils.unauthorizedResponse(exchange,"登录状态已过期");
        }
        String username = JwtUtils.getUserName(claims);
        String userCode = JwtUtils.getUserCode(claims);
        String manager = JwtUtils.getUserManger(claims);
        String userSuper = JwtUtils.getUserSuper(claims);
        String feishuOpenId = JwtUtils.getFeishuOpenId(claims);
        String deptId = JwtUtils.getDeptId(claims);
        if (StrUtil.isEmpty(userid) || StrUtil.isEmpty(username)) {
            return RespUtils.unauthorizedResponse(exchange, "令牌验证失败");
        }
        // 设置用户信息到请求
        WebFrameworkUtils.addHeader(mutate, SecurityConstants.DETAILS_USER_ID, userid);
        WebFrameworkUtils.addHeader(mutate, SecurityConstants.DETAILS_USERNAME, username);
        WebFrameworkUtils.addHeader(mutate, SecurityConstants.DETAILS_USER_CODE, userCode);
        WebFrameworkUtils.addHeader(mutate, SecurityConstants.DETAILS_IS_MANAGER, manager);
        WebFrameworkUtils.addHeader(mutate, SecurityConstants.DETAILS_IS_SUPER, userSuper);
        WebFrameworkUtils.addHeader(mutate, SecurityConstants.DETAILS_FEISHU_OPENID, feishuOpenId);
        WebFrameworkUtils.addHeader(mutate, SecurityConstants.DETAILS_DEPT_ID, deptId);
        WebFrameworkUtils.addHeader(mutate, "token", token);


        // 内部请求来源参数清除
        WebFrameworkUtils.removeHeader(mutate);
        return chain.filter(exchange.mutate().request(mutate.build()).build());
    }

    /**
     * 获取缓存key
     */
    private String getTokenKey(String userId, String clientType) {
        return SecurityConstants.USER_TYPE +userId+ clientType;
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
}
