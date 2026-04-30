package com.schemaplexai.spec.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.schemaplexai.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_spec_review")
public class SfSpecReview extends BaseEntity {

    private Long specId;
    private Long reviewerId;
    private String status;
    private String comment;
}
