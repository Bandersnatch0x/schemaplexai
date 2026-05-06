package com.schemaplexai.workflow.delegate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.common.context.TenantContextHolder;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.workflow.entity.SfWorkflowNodeExecution;
import com.schemaplexai.workflow.node.NodeExecutionResult;
import com.schemaplexai.workflow.service.WorkflowNodeEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Executes an AI agent task within the BPMN workflow.
 * Delegates to the WorkflowNodeEngine for actual agent execution,
 * and stores results back into process variables.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiAgentExecutionDelegate implements JavaDelegate {

    private final WorkflowNodeEngine nodeEngine;
    private final ObjectMapper objectMapper;

    @Override
    public void execute(DelegateExecution execution) {
        String processInstanceId = execution.getProcessInstanceId();
        String activityId = execution.getCurrentActivityId();
        String tenantId = resolveTenantId(execution);

        String agentId = (String) execution.getVariable("agentId");
        String taskDescription = (String) execution.getVariable("taskDescription");
        String executionTrackingId = (String) execution.getVariable("executionTrackingId");

        // Get retry count and human feedback if re-executing
        Integer retryCount = (Integer) execution.getVariable("retryCount");
        if (retryCount == null) {
            retryCount = 0;
        }
        String humanFeedback = (String) execution.getVariable("humanFeedback");

        log.info("[AiAgentExecute] process={}, activity={}, agentId={}, retry={}",
                processInstanceId, activityId, agentId, retryCount);

        try {
            // Build node execution input
            Map<String, Object> input = new HashMap<>();
            input.put("agentId", agentId);
            input.put("taskDescription", taskDescription);
            input.put("executionTrackingId", executionTrackingId);
            input.put("tenantId", tenantId);
            input.put("retryCount", retryCount);
            if (humanFeedback != null && !humanFeedback.isBlank()) {
                input.put("humanFeedback", humanFeedback);
            }

            // Build node execution record
            SfWorkflowNodeExecution nodeExecution = new SfWorkflowNodeExecution();
            nodeExecution.setNodeId(activityId);
            nodeExecution.setNodeType("AI_AGENT");
            nodeExecution.setInputJson(objectMapper.writeValueAsString(input));
            nodeExecution.setTenantId(tenantId);

            // Execute via node engine
            NodeExecutionResult result = nodeEngine.executeNode(nodeExecution);

            // Build agent result map
            Map<String, Object> agentResult = new HashMap<>();
            agentResult.put("success", result.isSuccess());
            agentResult.put("message", result.getMessage());
            agentResult.put("output", result.getOutput());
            agentResult.put("executedAt", Instant.now().toString());
            agentResult.put("activityId", activityId);

            // Compute a mock quality score based on output presence
            double qualityScore = computeQualityScore(result);
            agentResult.put("qualityScore", qualityScore);

            execution.setVariable("agentResult", agentResult);
            execution.setVariable("lastExecutionSuccess", result.isSuccess());

            if (result.isSuccess()) {
                execution.setVariable("retryCount", retryCount);
                log.info("[AiAgentExecute] Success process={}, qualityScore={}", processInstanceId, qualityScore);
            } else {
                execution.setVariable("retryCount", retryCount + 1);
                log.warn("[AiAgentExecute] Failed process={}, message={}", processInstanceId, result.getMessage());
            }

        } catch (Exception e) {
            log.error("[AiAgentExecute] Exception process={}, agentId={}", processInstanceId, agentId, e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("message", e.getMessage());
            errorResult.put("qualityScore", 0.0);
            execution.setVariable("agentResult", errorResult);
            execution.setVariable("lastExecutionSuccess", false);
            throw new BaseException(ResultCode.AGENT_EXECUTION_FAILED,
                    "AI agent execution failed: " + e.getMessage());
        }
    }

    private String resolveTenantId(DelegateExecution execution) {
        String tenantId = (String) execution.getVariable("tenantId");
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = TenantContextHolder.getTenantId();
            if (tenantId == null || tenantId.isBlank()) {
                tenantId = "default";
            }
        }
        return tenantId;
    }

    private double computeQualityScore(NodeExecutionResult result) {
        if (!result.isSuccess()) {
            return 0.0;
        }
        Map<String, Object> output = result.getOutput();
        if (output == null || output.isEmpty()) {
            return 0.3;
        }
        // Simple heuristic: more output fields = higher quality
        int fieldCount = output.size();
        return Math.min(0.5 + (fieldCount * 0.1), 1.0);
    }
}
