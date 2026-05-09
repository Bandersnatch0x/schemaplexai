package com.schemaplexai.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.schemaplexai.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_tenant_policy")
public class TenantPolicy extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableField("policy_type")
    private String policyType;

    @TableField("config_json")
    private String configJson;

    @TableField("enabled")
    private Boolean enabled;

    @Version
    @TableField("version")
    private Integer version;
}
