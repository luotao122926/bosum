package com.bosum.gateway.service;

import cn.hutool.core.util.ObjUtil;
import com.bosum.auditlog.common.domain.LogUserInfo;
import com.bosum.auditlog.producer.core.service.LogService;
import com.bosum.framework.common.constants.Constants;
import com.bosum.framework.common.constants.SecurityConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Map;

/**
 * @title: AuditService
 * @projectName erp-service-system
 * @description: 实现注入日志
 * @author zhubingbing
 * @date 2024/5/14 18:04
 */
@Slf4j
@Component
public class AuditService implements LogService {

    @Override
    public LogUserInfo getLogUserInfo(ServerHttpRequest request) {
        String userCode = request.getHeaders().getFirst(SecurityConstants.DETAILS_USER_CODE);
        String userName = request.getHeaders().getFirst(SecurityConstants.DETAILS_USERNAME);
        if(ObjUtil.isNotEmpty(userName)){
            try {
                userName = URLDecoder.decode(userName, Constants.UTF8);
            } catch (UnsupportedEncodingException e) {

            }
        }

        return new LogUserInfo(userName, userCode);
    }

    @Override
    public Map<String, Object> getExpendMsg(ServerHttpRequest request) {
        return null;
    }

    @Override
    public String getUniqueId(ServerHttpRequest request) {
        return request.getHeaders().getFirst("b-traceId-header");
    }

    @Override
    public String getPlatform() {
        return "erp-server-cloud";
    }



}

