package com.schemaplexai.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.schemaplexai.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_tenant")
public class SfTenant extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** sf_tenant is the tenant root table — it has no tenant_id column of its own. */
    @TableField(exist = false)
    private String tenantId;

    /** Not present on sf_tenant (root table predates audit columns). */
    @TableField(exist = false)
    private Long createdBy;

    /** Not present on sf_tenant (root table predates audit columns). */
    @TableField(exist = false)
    private Long updatedBy;

    @TableField("name")
    private String name;

    @TableField("code")
    private String code;

    @TableField("status")
    private String status;

    @TableField("config_json")
    private String configJson;
}
