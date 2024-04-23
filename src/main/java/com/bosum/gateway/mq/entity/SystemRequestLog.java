package com.bosum.gateway.mq.entity;


import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 系统接口请求日志采集
 */
@Data
public class SystemRequestLog {

    /**日志id*/
    private String logId;
    /**请求人code*/
    private String userCode;
    /** 请求人*/
    private String requestName;
    /** 请求时间*/
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date requestTime;
    /** 创建时间*/
    private String creationTime;
    /** 请求来源*/
    private String requestSourceType;
    /** 设备名*/
    private String equipmentName;
    /** 操作系统*/
    private String operatingSystem;
    /** 请求方式*/
    private String requestMethod;
    /*** 操作地点*/
    private String operLocation;
    /**访问实例*/
    private String targetServer;
    /**请求路径*/
    private String requestPath;
    /**请求与方法*/
    private String method;
    /**协议 */
    private String schema;
    /**请求体*/
    private String requestBody;
    /**响应体*/
    private String responseData;
    /**请求ip*/
    private String ip;
    /** 请求类型*/
    private String requestContentType;
    /**响应时间*/
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date responseTime;
    /**执行时间*/
    private long executeTime;
    /** 相应状态码*/
    private int code;
    /** 项目*/
    private String projectCloud;



}
