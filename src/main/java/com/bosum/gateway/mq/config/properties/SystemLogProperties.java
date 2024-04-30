package com.bosum.gateway.mq.config.properties;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;


@RefreshScope
@Component
@Data
public class SystemLogProperties {
    /**
     * 是否开启日志存储
     */
    @Value("${systemlog.enabled:false}")
    private Boolean enabled;

    /**
     * 项目名字
     */
    @Value("${systemlog.project-name:}")
    private String projectName;

}
