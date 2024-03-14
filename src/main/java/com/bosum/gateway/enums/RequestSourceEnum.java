package com.bosum.gateway.enums;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RequestSourceEnum {

    INNER("INNER", "内部服务访问标识"),
    NEW_ERP("NEW_ERP", "访问新架构的标识"),
    MOBILE_FROM("MOBILE_FROM", "客户问卷手机号登录验证标识"),
    ;
    private final String type;
    private final String description;

    public static RequestSourceEnum getRequestSourceEnumByType(String type) {
        for (RequestSourceEnum value : values()) {
            if(value.type.equals(type)){
                return value;
            }
        }
        return null ;
    }

    public static boolean judgeExist(String type) {
        for (RequestSourceEnum value : values()) {
            if(value.type.equals(type)){
                return true;
            }
        }
        return false ;
    }
}
