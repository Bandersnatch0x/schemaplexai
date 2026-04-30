package com.schemaplexai.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.schemaplexai.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_model_provider")
public class SfModelProvider extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableField("name")
    private String name;

    @TableField("code")
    private String code;

    @TableField("api_base_url")
    private String apiBaseUrl;

    @TableField("status")
    private Integer status;

    @TableField("rate_limit")
    private Integer rateLimit;
}
