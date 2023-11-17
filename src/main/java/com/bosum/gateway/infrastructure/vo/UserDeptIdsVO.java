package com.bosum.gateway.infrastructure.vo;


import lombok.Data;

import java.util.Set;

@Data
public class UserDeptIdsVO {

    private String userName;

    private String userId;

    private String userCode;

    private String superManager;

    private String manager;

    private Set<String> deptAuthList;
}
