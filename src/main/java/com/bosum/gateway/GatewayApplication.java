package com.bosum.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = {"com.bosum.*"},exclude= { DataSourceAutoConfiguration.class})
public class GatewayApplication {

    public static void main(String[] args) {
       try{
           SpringApplication.run(GatewayApplication.class, args);
       }catch (Exception e){
           e.printStackTrace();
       }
    }

}
