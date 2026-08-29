package com.schemaplexai.spec.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.schemaplexai.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_spec")
public class SfSpec extends BaseEntity {

    private String title;
    private String type;

    /**
     * Spec lifecycle status, stored as VARCHAR(32) in sf_spec (DDL default 'draft').
     * Unified vocabulary: draft / in_review / approved / published / archived.
     */
    private String status;
    private String content;
}
