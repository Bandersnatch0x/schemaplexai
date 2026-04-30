package com.schemaplexai.workflow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.schemaplexai.workflow.entity.SfWorkflowInstance;

public interface WorkflowInstanceService extends IService<SfWorkflowInstance> {

    void trigger(Long instanceId);
}
