package com.schemaplexai.quality.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.schemaplexai.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_quality_gate")
public class SfQualityGate extends BaseEntity {

    private String name;
    private String rulesJson;

    /**
     * Gate lifecycle status, stored as VARCHAR(32) in sf_quality_gate
     * (DDL default 'ACTIVE'). Allowed values: ACTIVE / INACTIVE / DEPRECATED.
     * Only ACTIVE gates participate in quality evaluation.
     */
    private String status;
}
