package com.schemaplexai.quality.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.schemaplexai.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_review_record")
public class SfReviewRecord extends BaseEntity {

    private String resourceType;
    private Long resourceId;
    private Long reviewerId;
    private Integer status;
    private String comment;
}
