package com.schemaplexai.quality.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.schemaplexai.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_quality_issue")
public class SfQualityIssue extends BaseEntity {

    private Long executionId;
    private String issueType;
    private String severity;
    private String description;

    /**
     * Issue handling status, stored as VARCHAR(32) in sf_quality_issue
     * (DDL default 'OPEN'). Allowed values:
     * OPEN / IN_PROGRESS / RESOLVED / CLOSED / WONT_FIX.
     */
    private String status;
}
