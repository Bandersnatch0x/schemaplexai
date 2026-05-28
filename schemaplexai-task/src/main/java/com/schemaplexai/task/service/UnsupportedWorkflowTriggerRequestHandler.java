package com.schemaplexai.task.service;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.task.mq.dto.WorkflowTriggerMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UnsupportedWorkflowTriggerRequestHandler implements WorkflowTriggerRequestHandler {

    @Override
    public void handle(WorkflowTriggerMessage message) {
        log.warn("[WorkflowTriggerRequestHandler] Workflow trigger received but no runtime adapter is configured: {}",
                message);
        throw new BaseException(ResultCode.INTERNAL_ERROR,
                "workflow trigger handler is not implemented for workflowDefinitionKey="
                        + message.getWorkflowDefinitionKey());
    }
}
