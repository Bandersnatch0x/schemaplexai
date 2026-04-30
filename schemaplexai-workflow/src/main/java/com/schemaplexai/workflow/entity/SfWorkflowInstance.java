package com.schemaplexai.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.schemaplexai.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_workflow_instance")
public class SfWorkflowInstance extends BaseEntity {

    private Long templateId;
    private String status;
    private String triggerType;
    private String triggerConfig;
}
