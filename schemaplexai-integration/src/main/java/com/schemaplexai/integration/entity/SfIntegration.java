package com.schemaplexai.integration.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.schemaplexai.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_integration")
public class SfIntegration extends BaseEntity {

    private String name;
    private String type;
    private String configJson;
    private Integer status;
}
