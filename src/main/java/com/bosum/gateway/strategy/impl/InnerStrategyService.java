package com.bosum.gateway.strategy.impl;


import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.bosum.framework.common.constants.SecurityConstants;
import com.bosum.gateway.enums.ProcessTypeEnumFlag;
import com.bosum.gateway.enums.RequestSourceEnum;
import com.bosum.gateway.infrastructure.OldErpHttpService;
import com.bosum.gateway.infrastructure.vo.UserDeptIdsVO;
import com.bosum.gateway.strategy.Strategy;
import com.bosum.gateway.util.WebFrameworkUtils;
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
 * @author zuhao.ouyang
 */
@Slf4j
@Service
@ProcessTypeEnumFlag(RequestSourceEnum.INNER)//自定义注解，标注该类为FOCUS_PROCESS
@RequiredArgsConstructor
public class InnerStrategyService implements Strategy {
    private final RedisTemplate<String, String> redisTemplate;

    private final OldErpHttpService oldErpHttpService;
    @Override
    public Mono<Void> check(ServerWebExchange exchange, GatewayFilterChain chain) {
        //要判断是否是新的erp2接口
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpRequest.Builder mutate = request.mutate();
        String userId = request.getHeaders().getFirst("User-Id");
        if (StrUtil.isNotEmpty(userId)) {
            try {
                //  远程调用老erp服务获取对应的用户信息 并且存放到请求头
                UserDeptIdsVO userInfo = oldErpHttpService.getUserInfo(userId);
                if (ObjectUtil.isNotNull(userInfo)) {
                    WebFrameworkUtils.addHeader(mutate, SecurityConstants.DETAILS_USER_ID, userInfo.getUserId());
                    WebFrameworkUtils.addHeader(mutate, SecurityConstants.DETAILS_USERNAME, userInfo.getUserName());
                    WebFrameworkUtils.addHeader(mutate, SecurityConstants.DETAILS_USER_CODE, userInfo.getUserCode());
                    WebFrameworkUtils.addHeader(mutate, SecurityConstants.DETAILS_IS_MANAGER, userInfo.getManager());
                    WebFrameworkUtils.addHeader(mutate, SecurityConstants.DETAILS_IS_SUPER, userInfo.getSuperManager());
                    WebFrameworkUtils.addHeader(mutate, SecurityConstants.DETAILS_DEPT_AUTH_LIST, userInfo.getDeptAuthList());
                }
            } catch (Exception e) {
                log.error("调用接口失败 ",e);
                // 为了不影响网关直接放行处理
                return chain.filter(exchange);
            }
        }
        return chain.filter(exchange.mutate().request(mutate.build()).build());
    }

}
