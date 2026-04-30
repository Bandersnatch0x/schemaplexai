package com.schemaplexai.integration.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.schemaplexai.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_api_gateway_config")
public class SfApiGatewayConfig extends BaseEntity {

    private String name;
    private String baseUrl;
    private String authType;
    private String authConfig;
    private Integer rateLimit;
}
