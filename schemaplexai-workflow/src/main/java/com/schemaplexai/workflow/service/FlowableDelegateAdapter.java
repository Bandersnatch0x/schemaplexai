package com.schemaplexai.workflow.service;

import com.schemaplexai.workflow.entity.SfWorkflowNodeExecution;
import com.schemaplexai.workflow.node.NodeExecutionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class FlowableDelegateAdapter implements JavaDelegate {

    private final WorkflowNodeEngine nodeEngine;

    @Override
    public void execute(DelegateExecution execution) {
        String processInstanceId = execution.getProcessInstanceId();
        String activityId = execution.getCurrentActivityId();
        log.info("Flowable ServiceTask executed for process: {}, activity: {}",
                processInstanceId, activityId);

        // Build a node execution from Flowable context
        SfWorkflowNodeExecution nodeExecution = new SfWorkflowNodeExecution();
        nodeExecution.setNodeId(activityId);
        nodeExecution.setNodeType((String) execution.getVariable("nodeType"));
        if (nodeExecution.getNodeType() == null) {
            nodeExecution.setNodeType("SCRIPT");
        }
        nodeExecution.setInputJson(buildInputJson(execution));

        NodeExecutionResult result = nodeEngine.executeNode(nodeExecution);
        execution.setVariable("nodeResult", result.isSuccess());
        execution.setVariable("nodeOutput", result.getOutput());
    }

    private String buildInputJson(DelegateExecution execution) {
        Map<String, Object> variables = execution.getVariables();
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(variables);
        } catch (Exception e) {
            log.warn("Failed to serialize Flowable variables to JSON", e);
            return "{}";
        }
    }
}
