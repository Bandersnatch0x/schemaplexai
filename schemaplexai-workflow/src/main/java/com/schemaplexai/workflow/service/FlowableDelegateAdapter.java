package com.schemaplexai.workflow.service;

import com.schemaplexai.common.context.TenantContextHolder;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.workflow.entity.SfWorkflowNodeExecution;
import com.schemaplexai.workflow.node.NodeExecutionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Bridges Flowable ServiceTasks to the WorkflowNodeEngine (spec §7). The engine persists
 * the node execution record (insert-if-new), so BPMN-path executions are auditable, and
 * tenant isolation is carried over from the process variables or the tenant context.
 */
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

        String nodeType = (String) execution.getVariable("nodeType");
        if (nodeType == null || nodeType.isBlank()) {
            // Silently defaulting used to route every unconfigured task into the script
            // executor; fail fast instead so misconfigured processes are visible.
            throw new BaseException(ResultCode.PARAM_ERROR,
                    "Flowable ServiceTask " + activityId + " requires a 'nodeType' process variable");
        }

        // Build a node execution from Flowable context
        SfWorkflowNodeExecution nodeExecution = new SfWorkflowNodeExecution();
        nodeExecution.setNodeId(activityId);
        nodeExecution.setNodeType(nodeType);
        nodeExecution.setTenantId(resolveTenantId(execution));
        Object workflowInstanceId = execution.getVariable("workflowInstanceId");
        if (workflowInstanceId instanceof Number number) {
            nodeExecution.setInstanceId(number.longValue());
        }
        nodeExecution.setInputJson(buildInputJson(execution));

        NodeExecutionResult result = nodeEngine.executeNode(nodeExecution);
        execution.setVariable("nodeResult", result.isSuccess());
        execution.setVariable("nodeOutput", result.getOutput());
    }

    private String resolveTenantId(DelegateExecution execution) {
        Object tenantVariable = execution.getVariable("tenantId");
        if (tenantVariable != null && !tenantVariable.toString().isBlank()) {
            return tenantVariable.toString();
        }
        String tenantId = TenantContextHolder.getTenantId();
        // The node execution row requires a tenant (tenant_id NOT NULL); mirror the
        // AiAgentExecutionDelegate fallback so the bridge never inserts without one.
        return (tenantId == null || tenantId.isBlank()) ? "default" : tenantId;
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
