package com.schemaplexai.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.schemaplexai.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_permission")
public class SfPermission extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableField("tenant_id")
    private String tenantId;

    @TableField("name")
    private String name;

    @TableField("code")
    private String code;

    @TableField("type")
    private String type;
}
