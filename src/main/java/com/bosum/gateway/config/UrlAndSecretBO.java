package com.bosum.gateway.config;


import lombok.Data;

import java.util.List;

@Data
public class UrlAndSecretBO {
    private String name;
    private String secret;
    private List<String> url;
}
