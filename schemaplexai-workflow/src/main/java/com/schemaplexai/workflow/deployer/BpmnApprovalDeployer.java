package com.schemaplexai.workflow.deployer;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Deployer and runtime operator for the agent-execution-approval BPMN process.
 *
 * <p>Provides a focused API for starting approval workflows and completing
 * human tasks, abstracting the underlying Flowable RuntimeService and TaskService.
 *
 * <p>The BPMN process is auto-deployed by {@link com.schemaplexai.workflow.service.WorkflowDeployService}
 * on application startup from {@code classpath:processes/agent-execution-approval.bpmn20.xml}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BpmnApprovalDeployer {

    private static final String PROCESS_DEFINITION_KEY = "agentExecutionApproval";

    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final RepositoryService repositoryService;

    /**
     * Starts a new agent execution approval process instance.
     *
     * @param ticketId  the approval ticket ID (used as businessKey for correlation)
     * @param variables process variables: ticketId, executionId, tenantId, agentId,
     *                  riskLevel, actionDescription, approverId, etc.
     * @return the started process instance ID
     * @throws BaseException if the process definition is not found
     */
    public String startApprovalProcess(String ticketId, Map<String, Object> variables) {
        ProcessDefinition definition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(PROCESS_DEFINITION_KEY)
                .active()
                .latestVersion()
                .singleResult();

        if (definition == null) {
            throw new BaseException(ResultCode.WORKFLOW_NOT_FOUND,
                    "Approval process definition not found: " + PROCESS_DEFINITION_KEY);
        }

        ProcessInstance instance = runtimeService
                .startProcessInstanceByKey(PROCESS_DEFINITION_KEY, ticketId, variables);

        log.info("[BpmnApprovalDeployer] Started approval process instanceId={}, ticketId={}, definitionKey={}",
                instance.getId(), ticketId, PROCESS_DEFINITION_KEY);

        return instance.getId();
    }

    /**
     * Completes an active human approval task with the given decision.
     *
     * @param taskId   the Flowable task ID
     * @param approved true for APPROVE, false for REJECT
     * @param reason   optional rejection reason (ignored when approved)
     * @throws BaseException if the task is not found or not assigned
     */
    public void completeTask(String taskId, boolean approved, String reason) {
        Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();

        if (task == null) {
            throw new BaseException(ResultCode.WORKFLOW_NOT_FOUND,
                    "Approval task not found: " + taskId);
        }

        String decision = approved ? "APPROVE" : "REJECT";
        taskService.setVariable(taskId, "approvalDecision", decision);

        if (!approved && reason != null && !reason.isBlank()) {
            taskService.setVariable(taskId, "rejectionReason", reason);
        }

        taskService.complete(taskId);

        log.info("[BpmnApprovalDeployer] Completed taskId={}, decision={}, processInstanceId={}",
                taskId, decision, task.getProcessInstanceId());
    }

    /**
     * Finds the active approval task for a given process instance.
     *
     * @param processInstanceId the process instance ID
     * @return the active task, or null if none found
     */
    public Task findActiveTask(String processInstanceId) {
        return taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .active()
                .singleResult();
    }
}
