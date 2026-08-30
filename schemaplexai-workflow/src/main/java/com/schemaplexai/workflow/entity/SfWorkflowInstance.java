package com.schemaplexai.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.schemaplexai.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_workflow_instance")
public class SfWorkflowInstance extends BaseEntity {

    private Long templateId;
    private String status;
    private String triggerType;
    private String triggerConfig;
    private String topologyHash;
    /** Instance-level input parameters (spec §5.2 input_data); seeds the ${input.xxx} substitution context. */
    private String inputData;
    /** Merged node outputs once the instance completes (spec §5.2 output_data). */
    private String outputData;
    /** Execution start time (spec §5.2 start_time); set when the instance turns RUNNING. */
    private LocalDateTime startedAt;
    /** Execution end time (spec §5.2 end_time); set on any terminal status. */
    private LocalDateTime completedAt;
}
