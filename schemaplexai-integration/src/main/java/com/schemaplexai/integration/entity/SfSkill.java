package com.schemaplexai.integration.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.schemaplexai.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_skill")
public class SfSkill extends BaseEntity {

    private String name;
    private String code;
    private String description;
    private String content;
    private Integer status;
}
