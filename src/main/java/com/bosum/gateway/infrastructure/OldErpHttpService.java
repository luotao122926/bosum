package com.bosum.gateway.infrastructure;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.bosum.framework.common.core.ResultData;
import com.bosum.framework.security.utils.RestTemplateUtils;
import com.bosum.gateway.infrastructure.vo.UserDeptIdsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OldErpHttpService {


    private final RestTemplateUtils restTemplateUtils;


    public UserDeptIdsVO getUserInfo(String userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Requestsource","INNER");
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("userId", userId);
        ResultData resultData = restTemplateUtils.get(RestTemplateUtils.ERP_BASE_URL + "dept/getDeptIdsByUserId", paramMap, headers);
        if (resultData.isSuccess()) {
            Object data = resultData.getData();
            return JSONObject.toJavaObject((JSON) JSON.toJSON(data), UserDeptIdsVO.class);
        }

        return null ;
    }

}
