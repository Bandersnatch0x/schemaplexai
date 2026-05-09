package com.schemaplexai.workflow.deployer;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.repository.ProcessDefinitionQuery;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link BpmnApprovalDeployer}.
 *
 * <p>Covers: deploy verification, start approval process, complete task (approve/reject),
 * and active task lookup.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("M7.2: BpmnApprovalDeployer Tests")
class BpmnApprovalDeployerTest {

    @Mock
    private RuntimeService runtimeService;

    @Mock
    private TaskService taskService;

    @Mock
    private RepositoryService repositoryService;

    @Mock
    private ProcessDefinitionQuery processDefinitionQuery;

    @Mock
    private TaskQuery taskQuery;

    @InjectMocks
    private BpmnApprovalDeployer deployer;

    // ------------------------------------------------------------------
    // startApprovalProcess
    // ------------------------------------------------------------------

    @Test
    @DisplayName("startApprovalProcess: returns processInstanceId with ticketId as businessKey")
    void startApprovalProcess_success_returnsInstanceId() {
        String ticketId = UUID.randomUUID().toString();
        Map<String, Object> variables = Map.of(
                "ticketId", ticketId,
                "executionId", 1001L,
                "riskLevel", "HIGH"
        );

        ProcessDefinition definition = mock(ProcessDefinition.class);
        when(repositoryService.createProcessDefinitionQuery()).thenReturn(processDefinitionQuery);
        when(processDefinitionQuery.processDefinitionKey("agentExecutionApproval")).thenReturn(processDefinitionQuery);
        when(processDefinitionQuery.active()).thenReturn(processDefinitionQuery);
        when(processDefinitionQuery.latestVersion()).thenReturn(processDefinitionQuery);
        when(processDefinitionQuery.singleResult()).thenReturn(definition);

        ProcessInstance instance = mock(ProcessInstance.class);
        when(instance.getId()).thenReturn("proc-inst-123");
        when(runtimeService.startProcessInstanceByKey(eq("agentExecutionApproval"), eq(ticketId), eq(variables)))
                .thenReturn(instance);

        String result = deployer.startApprovalProcess(ticketId, variables);

        assertThat(result).isEqualTo("proc-inst-123");
        verify(runtimeService).startProcessInstanceByKey("agentExecutionApproval", ticketId, variables);
    }

    @Test
    @DisplayName("startApprovalProcess: throws WORKFLOW_NOT_FOUND when definition missing")
    void startApprovalProcess_definitionNotFound_throws() {
        String ticketId = UUID.randomUUID().toString();

        when(repositoryService.createProcessDefinitionQuery()).thenReturn(processDefinitionQuery);
        when(processDefinitionQuery.processDefinitionKey("agentExecutionApproval")).thenReturn(processDefinitionQuery);
        when(processDefinitionQuery.active()).thenReturn(processDefinitionQuery);
        when(processDefinitionQuery.latestVersion()).thenReturn(processDefinitionQuery);
        when(processDefinitionQuery.singleResult()).thenReturn(null);

        assertThatThrownBy(() -> deployer.startApprovalProcess(ticketId, Map.of()))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.WORKFLOW_NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("startApprovalProcess: passes empty variables map correctly")
    void startApprovalProcess_emptyVariables_stillStarts() {
        String ticketId = UUID.randomUUID().toString();

        ProcessDefinition definition = mock(ProcessDefinition.class);
        when(repositoryService.createProcessDefinitionQuery()).thenReturn(processDefinitionQuery);
        when(processDefinitionQuery.processDefinitionKey("agentExecutionApproval")).thenReturn(processDefinitionQuery);
        when(processDefinitionQuery.active()).thenReturn(processDefinitionQuery);
        when(processDefinitionQuery.latestVersion()).thenReturn(processDefinitionQuery);
        when(processDefinitionQuery.singleResult()).thenReturn(definition);

        ProcessInstance instance = mock(ProcessInstance.class);
        when(instance.getId()).thenReturn("proc-inst-456");
        when(runtimeService.startProcessInstanceByKey(eq("agentExecutionApproval"), eq(ticketId), any(Map.class)))
                .thenReturn(instance);

        String result = deployer.startApprovalProcess(ticketId, Map.of());

        assertThat(result).isEqualTo("proc-inst-456");
    }

    // ------------------------------------------------------------------
    // completeTask
    // ------------------------------------------------------------------

    @Test
    @DisplayName("completeTask: approve sets APPROVE decision and completes task")
    void completeTask_approve_setsApproveAndCompletes() {
        String taskId = "task-123";

        Task task = mock(Task.class);
        when(task.getProcessInstanceId()).thenReturn("proc-1");
        when(taskService.createTaskQuery()).thenReturn(taskQuery);
        when(taskQuery.taskId(taskId)).thenReturn(taskQuery);
        when(taskQuery.singleResult()).thenReturn(task);

        deployer.completeTask(taskId, true, null);

        verify(taskService).setVariable(taskId, "approvalDecision", "APPROVE");
        verify(taskService, never()).setVariable(eq(taskId), eq("rejectionReason"), any());
        verify(taskService).complete(taskId);
    }

    @Test
    @DisplayName("completeTask: reject sets REJECT decision, stores reason, and completes task")
    void completeTask_reject_setsRejectAndReasonAndCompletes() {
        String taskId = "task-456";

        Task task = mock(Task.class);
        when(task.getProcessInstanceId()).thenReturn("proc-2");
        when(taskService.createTaskQuery()).thenReturn(taskQuery);
        when(taskQuery.taskId(taskId)).thenReturn(taskQuery);
        when(taskQuery.singleResult()).thenReturn(task);

        deployer.completeTask(taskId, false, "Security concern");

        verify(taskService).setVariable(taskId, "approvalDecision", "REJECT");
        verify(taskService).setVariable(taskId, "rejectionReason", "Security concern");
        verify(taskService).complete(taskId);
    }

    @Test
    @DisplayName("completeTask: reject with blank reason does not store rejectionReason")
    void completeTask_reject_blankReason_doesNotStoreReason() {
        String taskId = "task-789";

        Task task = mock(Task.class);
        when(task.getProcessInstanceId()).thenReturn("proc-3");
        when(taskService.createTaskQuery()).thenReturn(taskQuery);
        when(taskQuery.taskId(taskId)).thenReturn(taskQuery);
        when(taskQuery.singleResult()).thenReturn(task);

        deployer.completeTask(taskId, false, "");

        verify(taskService).setVariable(taskId, "approvalDecision", "REJECT");
        verify(taskService, never()).setVariable(eq(taskId), eq("rejectionReason"), any());
        verify(taskService).complete(taskId);
    }

    @Test
    @DisplayName("completeTask: reject with null reason does not store rejectionReason")
    void completeTask_reject_nullReason_doesNotStoreReason() {
        String taskId = "task-abc";

        Task task = mock(Task.class);
        when(task.getProcessInstanceId()).thenReturn("proc-4");
        when(taskService.createTaskQuery()).thenReturn(taskQuery);
        when(taskQuery.taskId(taskId)).thenReturn(taskQuery);
        when(taskQuery.singleResult()).thenReturn(task);

        deployer.completeTask(taskId, false, null);

        verify(taskService).setVariable(taskId, "approvalDecision", "REJECT");
        verify(taskService, never()).setVariable(eq(taskId), eq("rejectionReason"), any());
        verify(taskService).complete(taskId);
    }

    @Test
    @DisplayName("completeTask: task not found throws WORKFLOW_NOT_FOUND")
    void completeTask_taskNotFound_throws() {
        String taskId = "task-missing";

        when(taskService.createTaskQuery()).thenReturn(taskQuery);
        when(taskQuery.taskId(taskId)).thenReturn(taskQuery);
        when(taskQuery.singleResult()).thenReturn(null);

        assertThatThrownBy(() -> deployer.completeTask(taskId, true, null))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.WORKFLOW_NOT_FOUND.getCode());

        verify(taskService, never()).complete(any());
    }

    // ------------------------------------------------------------------
    // findActiveTask
    // ------------------------------------------------------------------

    @Test
    @DisplayName("findActiveTask: returns active task for process instance")
    void findActiveTask_found_returnsTask() {
        String processInstanceId = "proc-5";
        Task expectedTask = mock(Task.class);

        when(taskService.createTaskQuery()).thenReturn(taskQuery);
        when(taskQuery.processInstanceId(processInstanceId)).thenReturn(taskQuery);
        when(taskQuery.active()).thenReturn(taskQuery);
        when(taskQuery.singleResult()).thenReturn(expectedTask);

        Task result = deployer.findActiveTask(processInstanceId);

        assertThat(result).isEqualTo(expectedTask);
    }

    @Test
    @DisplayName("findActiveTask: returns null when no active task")
    void findActiveTask_notFound_returnsNull() {
        String processInstanceId = "proc-6";

        when(taskService.createTaskQuery()).thenReturn(taskQuery);
        when(taskQuery.processInstanceId(processInstanceId)).thenReturn(taskQuery);
        when(taskQuery.active()).thenReturn(taskQuery);
        when(taskQuery.singleResult()).thenReturn(null);

        Task result = deployer.findActiveTask(processInstanceId);

        assertThat(result).isNull();
    }
}
