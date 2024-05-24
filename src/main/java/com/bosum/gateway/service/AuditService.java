package com.bosum.gateway.service;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.bosum.auditlog.common.domain.LogUserInfo;
import com.bosum.auditlog.producer.core.service.LogService;
import com.bosum.framework.common.constants.Constants;
import com.bosum.framework.common.constants.SecurityConstants;
import com.bosum.framework.common.util.MDCTraceUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
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


    @Autowired
    Environment environment;

    @Override
    public LogUserInfo getLogUserInfo(ServerHttpRequest request, ServerHttpResponse response) {
        String userCode = request.getHeaders().getFirst(SecurityConstants.DETAILS_USER_CODE);
        String userName = request.getHeaders().getFirst(SecurityConstants.DETAILS_USERNAME);
        String auditLogId = request.getHeaders().getFirst(MDCTraceUtil.TRACE_ID_HEADER);
        // 从原ERP提取登录信息
        if(ObjUtil.isEmpty(userCode) && ObjUtil.isNotEmpty(auditLogId)){
            String userJson = response.getHeaders().getFirst(auditLogId);
            if(ObjUtil.isNotEmpty(userJson)){
                JSONObject jsonObject = JSONUtil.parseObj(userJson);
                userCode = jsonObject.getStr("userCode");
                userName = jsonObject.getStr("userName");
            }
        }

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
        Map<String, Object> hashMap = new HashMap<>();
        hashMap.put("env", environment.getActiveProfiles()[0]);
        return hashMap;
    }

    @Override
    public String getUniqueId(ServerHttpRequest request) {
        return request.getHeaders().getFirst(MDCTraceUtil.TRACE_ID_HEADER);
    }

    @Override
    public String getPlatform() {
        return "erp-server-cloud";
    }



}

