package com.schemaplexai.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.schemaplexai.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_workflow_node_execution")
public class SfWorkflowNodeExecution extends BaseEntity {

    private Long instanceId;
    private String nodeId;
    private String nodeType;
    private String status;
    private String inputJson;
    private String outputJson;
}
