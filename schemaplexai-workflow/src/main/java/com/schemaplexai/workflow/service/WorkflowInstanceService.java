package com.schemaplexai.workflow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.schemaplexai.workflow.entity.SfWorkflowInstance;

public interface WorkflowInstanceService extends IService<SfWorkflowInstance> {

    void trigger(Long instanceId);

    /** Cancels a PENDING / RUNNING / WAITING_APPROVAL instance (spec §6.2, §4 CANCELLED). */
    void cancel(Long instanceId);

    /** Approves the pending HUMAN_APPROVAL gate and resumes execution (spec §6.2). */
    void approve(Long instanceId, String comment);

    /** Rejects the pending HUMAN_APPROVAL gate, failing the instance (spec §6.2). */
    void reject(Long instanceId, String reason);
}
