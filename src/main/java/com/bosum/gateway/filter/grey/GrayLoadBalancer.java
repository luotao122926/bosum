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
 * @author zhubingbing
 * @date 2024/5/28 9:37
 */
@Slf4j
@RequiredArgsConstructor
public class GrayLoadBalancer implements ReactorServiceInstanceLoadBalancer {

    /** 灰度发布标识  */
    public static final String VERSION = "Gray-version";

    /** 本地调试标识，如果采用灰度发布，可以采用新的标识 */
    public static final String LOCAL_DEBUG = "Local-debug";

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
        String localDebug = headers.getFirst(LOCAL_DEBUG);
        List<ServiceInstance> chooseInstances = CollectionUtils.filterList(instanceList, instance -> {
            String metaData = instance.getMetadata().get(LOCAL_DEBUG);
            return StrUtil.isNotEmpty(localDebug) ? StrUtil.equals(localDebug, metaData) : StrUtil.isEmpty(metaData);
        });

        // 如果是本地debug模式，采取降级
        if(!StrUtil.isEmpty(localDebug) && CollUtil.isEmpty(chooseInstances)){
            log.warn("[getInstanceResponse][serviceId({}) 没有满足版本({})的服务实例列表，直接使用所有服务实例列表]", serviceId, localDebug);
            chooseInstances = instanceList;
        }

        // tag 非空, 不能降级
        if (CollUtil.isEmpty(chooseInstances)) {
            log.warn("[getInstanceResponse][serviceId({}) 服务实例列表为空]", serviceId);
            return new EmptyResponse();
        }

        return new DefaultResponse(NacosBalancer.getHostByRandomWeight3(chooseInstances));
    }

}

