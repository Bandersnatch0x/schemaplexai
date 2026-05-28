package com.schemaplexai.task.service;

import com.schemaplexai.task.mq.dto.WorkflowTriggerMessage;

public interface WorkflowTriggerRequestHandler {

    void handle(WorkflowTriggerMessage message);
}
