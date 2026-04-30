package com.schemaplexai.quality.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.schemaplexai.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_security_policy")
public class SfSecurityPolicy extends BaseEntity {

    private String name;
    private String policyType;
    private String rulesJson;
    private Integer status;
}
