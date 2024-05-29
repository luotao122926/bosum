package com.bosum.gateway.filter.grey;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.nacos.balancer.NacosBalancer;
import com.bosum.framework.common.util.collection.CollectionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.DefaultResponse;
import org.springframework.cloud.client.loadbalancer.EmptyResponse;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.RequestDataContext;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.loadbalancer.core.NoopServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.http.HttpHeaders;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * @title: GrayLoadBalancer
 * @projectName erp-service-system
 * @description:
 *  灰度控制类
 *  根据header中的标记进行筛选
 *
 *
 *
 * @author zhubingbing
 * @date 2024/5/28 9:37
 */
@Slf4j
@RequiredArgsConstructor
public class GrayLoadBalancer implements ReactorServiceInstanceLoadBalancer {

    /** 标记属性 */
    public static final String VERSION = "Gray-version";

    /** 实例列表 */
    private final ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider;

    private final String serviceId;

    @Override
    public Mono<Response<ServiceInstance>> choose(Request request) {
        HttpHeaders headers = ((RequestDataContext) request.getContext()).getClientRequest().getHeaders();
        ServiceInstanceListSupplier supplier = serviceInstanceListSupplierProvider.getIfAvailable(NoopServiceInstanceListSupplier::new);
        return supplier.get(request).next().map(list -> getInstanceResponse(list, headers));
    }

    private Response<ServiceInstance> getInstanceResponse(List<ServiceInstance> instanceList, HttpHeaders headers){
        if(CollUtil.isEmpty(instanceList)){
            log.warn("[getInstanceResponse][serviceId({}) 服务实例列表为空]", serviceId);
            return new EmptyResponse();
        }

        // 根据条件筛选
        String version = headers.getFirst(VERSION);
        List<ServiceInstance> chooseInstances = null;
        // 如果没有注入筛选条件，则注入所有实例
        if(!StrUtil.isEmpty(version)){
            chooseInstances =  CollectionUtils.filterList(instanceList, instance -> StrUtil.equals(version, instance.getMetadata().get(VERSION)));
        }

        if(CollUtil.isEmpty(chooseInstances)){
            if(!StrUtil.isEmpty(version)){
                log.warn("[getInstanceResponse][serviceId({}) 没有满足版本({})的服务实例列表，直接使用所有服务实例列表]", serviceId, version);
            }

            chooseInstances = instanceList;
        }
        return new DefaultResponse(NacosBalancer.getHostByRandomWeight3(chooseInstances));
    }

}

