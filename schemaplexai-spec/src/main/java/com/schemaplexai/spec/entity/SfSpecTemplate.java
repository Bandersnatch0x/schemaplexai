package com.schemaplexai.spec.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.schemaplexai.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_spec_template")
public class SfSpecTemplate extends BaseEntity {

    private String name;
    private String content;
    private String category;
}
