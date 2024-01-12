package com.bosum.gateway.service;

import cn.hutool.core.util.RandomUtil;
import com.alibaba.cloud.nacos.discovery.NacosServiceDiscovery;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.api.exception.NacosException;
import com.bosum.framework.common.core.ResultData;
import com.bosum.framework.common.exception.ServiceException;
import com.bosum.framework.common.util.validation.ValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;

/**
 * @title: UserLoginService
 * @projectName erp-service
 * @description: 内部服务登录
 * @author zhubingbing
 * @date 2023/12/21 15:04
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InnerUserLoginService {

    private static final String SYSTEM_SERVICE = "erp-service-system";

    private final NacosServiceDiscovery nacosServiceDiscovery;

    public JSONObject login(String userId) {
        String url = getServiceUrl() + "/system/user/getUserAuthById?userId=" + userId;
        log.info("");
        RestTemplate restTemplate = new RestTemplate();
        try {
            ResultData resultData = restTemplate.getForObject(new URI(url), ResultData.class);
            if (!resultData.isSuccess()) {
                throw new RuntimeException("用户信息获取失败, 禁止访问");
            }

            String resultDataStr = JSON.toJSONString(resultData.getData());
            log.info("获取登录用户信息:{}", resultDataStr);
            return JSON.parseObject(resultDataStr);
        }catch (Exception e){
            log.error("访问{}异常 => {}", SYSTEM_SERVICE, e.getMessage());
            throw new ServiceException("访问"+SYSTEM_SERVICE+"异常");
        }
    }

    private String getServiceUrl(){
        String url = "http://127.0.0.1";
        try {
            List<ServiceInstance> instanceList = nacosServiceDiscovery.getInstances(SYSTEM_SERVICE);
            if(ValidationUtil.isEmpty(instanceList)){
                throw new ServiceException("没有发现"+SYSTEM_SERVICE+"实例");
            }
            ServiceInstance serviceInstance = RandomUtil.randomEle(instanceList);
            url = "http://" + serviceInstance.getHost() + ":" + serviceInstance.getPort();
        } catch (NacosException e) {
            throw new ServiceException("获取"+SYSTEM_SERVICE+"实例异常");
        }

        return url;
    }

}

