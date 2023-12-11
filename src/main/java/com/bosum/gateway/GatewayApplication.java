package com.bosum.gateway;

import com.bosum.framework.security.utils.RestTemplateUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestTemplate;


@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = {"com.bosum.*"},exclude= {DataSourceAutoConfiguration.class})
@Import(value = {RestTemplate.class , RestTemplateUtils.class})
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

}
