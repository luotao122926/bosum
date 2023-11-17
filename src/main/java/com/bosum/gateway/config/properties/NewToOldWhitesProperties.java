package com.bosum.gateway.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@RefreshScope
@ConfigurationProperties(prefix = "whites-rules.ignore.new-to-old-whites")
@Data
public class NewToOldWhitesProperties {
    private String secret;
    private List<String> url;
}
