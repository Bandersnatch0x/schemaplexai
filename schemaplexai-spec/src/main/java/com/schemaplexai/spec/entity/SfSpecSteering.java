package com.schemaplexai.spec.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.schemaplexai.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_spec_steering")
public class SfSpecSteering extends BaseEntity {

    private Long specId;
    private String direction;
    private String constraints;
    private String acceptanceCriteria;
}
