package com.schemaplexai.quality.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.schemaplexai.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_tool_approval_amendment")
public class SfToolApprovalAmendment extends BaseEntity {

    private String toolName;
    private Integer approvalThreshold;
    private Integer currentCount;
    private Integer status;
}
