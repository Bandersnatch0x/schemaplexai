package com.schemaplexai.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.schemaplexai.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_workflow_template")
public class SfWorkflowTemplate extends BaseEntity {

    private String name;
    private String description;
    private String nodeConfigJson;
    private String status;
}
