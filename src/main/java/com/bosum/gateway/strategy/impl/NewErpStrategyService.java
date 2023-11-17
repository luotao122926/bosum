package com.bosum.gateway.strategy.impl;


import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import com.bosum.framework.common.constants.SecurityConstants;
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

/**
 * 具体策略（ConcreteStrategy）：具体策略是实现策略接口的类。
 *  @author zuhao.ouyang
 */
@Slf4j
@Service
@ProcessTypeEnumFlag(RequestSourceEnum.NEW_ERP)//自定义注解，标注该类为FOCUS_PROCESS
@RequiredArgsConstructor
public class NewErpStrategyService implements Strategy {


    private final RedisTemplate<String, String> redisTemplate;
    @Override
    public Mono<Void> check(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpRequest.Builder mutate = request.mutate();
        String clientType = request.getHeaders().getFirst("CLIENT_TYPE");
        String token = getToken(request);
        if (StrUtil.isEmpty(token)) {
            return RespUtils.unauthorizedResponse(exchange, "令牌不能为空");
        }
        Claims claims = JwtUtils.parseToken(token);

        if (claims == null) {
            return RespUtils.unauthorizedResponse(exchange, "令牌已过期或验证不正确！");
        }
        String userid = JwtUtils.getUserId(claims);
        boolean isLogin = Boolean.TRUE.equals(redisTemplate.hasKey(getTokenKey(userid,clientType)));
        if (!isLogin) {
            return RespUtils.unauthorizedResponse(exchange, "登录状态已过期");
        }
        String username = JwtUtils.getUserName(claims);
        String userCode = JwtUtils.getUserCode(claims);
        String manager = JwtUtils.getUserManger(claims);
        String userSuper = JwtUtils.getUserSuper(claims);
        if (StrUtil.isEmpty(userid) || StrUtil.isEmpty(username)) {
            return RespUtils.unauthorizedResponse(exchange, "令牌验证失败");
        }
        // 设置用户信息到请求
        WebFrameworkUtils.addHeader(mutate, SecurityConstants.DETAILS_USER_ID, userid);
        WebFrameworkUtils.addHeader(mutate, SecurityConstants.DETAILS_USERNAME, username);
        WebFrameworkUtils.addHeader(mutate, SecurityConstants.DETAILS_USER_CODE, userCode);
        WebFrameworkUtils.addHeader(mutate, SecurityConstants.DETAILS_IS_MANAGER, manager);
        WebFrameworkUtils.addHeader(mutate, SecurityConstants.DETAILS_IS_SUPER, userSuper);
        // 从redis获取
        WebFrameworkUtils.addHeader(mutate, SecurityConstants.DETAILS_DEPT_AUTH_LIST, redisTemplate.opsForValue().get(SecurityConstants.NEW_ERP_DEPT_ID_LIST + userid));
        // 内部请求来源参数清除
        WebFrameworkUtils.removeHeader(mutate);
        removeHeader(mutate);
        return chain.filter(exchange.mutate().request(mutate.build()).build());
    }

    private void addHeader(ServerHttpRequest.Builder mutate, String name, Object value) {
        if (value == null) {
            return;
        }
        String valueStr = value.toString();
        String valueEncode = URLUtil.encode(valueStr);
        mutate.header(name, valueEncode);
    }

    private void removeHeader(ServerHttpRequest.Builder mutate) {
        mutate.headers(httpHeaders -> httpHeaders.remove(SecurityConstants.REQUEST_SOURCE)).build();
        mutate.headers(httpHeaders -> httpHeaders.remove("USER_ID")).build();
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
