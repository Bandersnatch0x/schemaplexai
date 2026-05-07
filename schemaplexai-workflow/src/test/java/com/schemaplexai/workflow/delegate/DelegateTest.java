package com.schemaplexai.workflow.delegate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.common.context.TenantContextHolder;
import com.schemaplexai.workflow.node.NodeExecutionResult;
import com.schemaplexai.workflow.service.WorkflowNodeEngine;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.task.service.delegate.DelegateTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DelegateTest {

    @Mock
    private DelegateExecution execution;

    @Mock
    private WorkflowNodeEngine nodeEngine;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private SpecReviewInitDelegate specReviewInitDelegate;

    @InjectMocks
    private SpecReviewNotificationDelegate specReviewNotificationDelegate;

    @InjectMocks
    private AiAgentInitDelegate aiAgentInitDelegate;

    @InjectMocks
    private AiAgentExecutionDelegate aiAgentExecutionDelegate;

    @InjectMocks
    private AiAgentResultProcessorDelegate aiAgentResultProcessorDelegate;

    @BeforeEach
    void setUp() {
        TenantContextHolder.clear();
    }

    // ========== SpecReviewInitDelegate ==========

    @Test
    void specReviewInit_withBlankTenantId_setsDefault() {
        when(execution.getProcessInstanceId()).thenReturn("proc-1");
        when(execution.getProcessInstanceBusinessKey()).thenReturn("biz-1");
        when(execution.getVariable("tenantId")).thenReturn("");
        when(execution.getVariable("reviewTrackingId")).thenReturn(null);
        when(execution.getVariable("riskLevel")).thenReturn(null);
        when(execution.getVariable("reviewerId")).thenReturn(null);

        specReviewInitDelegate.execute(execution);

        verify(execution).setVariable("tenantId", "default");
        verify(execution).setVariable(eq("reviewTrackingId"), any(String.class));
        verify(execution).setVariable("riskLevel", "MEDIUM");
        verify(execution).setVariable("reviewerId", "reviewer-group");
        verify(execution).setVariable(eq("submittedAt"), any(String.class));
        verify(execution).setVariable("approvalDecision", "PENDING");
    }

    @Test
    void specReviewInit_withExistingValues_preservesThem() {
        when(execution.getProcessInstanceId()).thenReturn("proc-1");
        when(execution.getProcessInstanceBusinessKey()).thenReturn("biz-1");
        when(execution.getVariable("tenantId")).thenReturn("tenant-a");
        when(execution.getVariable("reviewTrackingId")).thenReturn("track-1");
        when(execution.getVariable("riskLevel")).thenReturn("HIGH");
        when(execution.getVariable("reviewerId")).thenReturn("user-1");

        specReviewInitDelegate.execute(execution);

        verify(execution, never()).setVariable(eq("tenantId"), any());
        verify(execution, never()).setVariable(eq("reviewTrackingId"), any());
        verify(execution, never()).setVariable(eq("riskLevel"), any());
        verify(execution, never()).setVariable(eq("reviewerId"), any());
    }

    @Test
    void specReviewInit_notify_delegatesToExecute() {
        when(execution.getProcessInstanceId()).thenReturn("proc-1");
        when(execution.getProcessInstanceBusinessKey()).thenReturn("biz-1");
        when(execution.getVariable("tenantId")).thenReturn("t1");
        when(execution.getVariable("reviewTrackingId")).thenReturn("trk");
        when(execution.getVariable("riskLevel")).thenReturn("LOW");
        when(execution.getVariable("reviewerId")).thenReturn("r1");

        specReviewInitDelegate.notify(execution);

        verify(execution, never()).setVariable(eq("tenantId"), any());
    }

    // ========== SpecReviewNotificationDelegate ==========

    @Test
    void specReviewNotify_autoApproveTask() throws Exception {
        when(execution.getProcessInstanceId()).thenReturn("proc-1");
        when(execution.getCurrentActivityId()).thenReturn("autoApproveTask");
        when(execution.getVariable("tenantId")).thenReturn("t1");
        when(execution.getVariable("specId")).thenReturn("spec-1");
        when(execution.getVariable("specTitle")).thenReturn("My Spec");
        when(execution.getVariable("submitterId")).thenReturn("user-1");
        when(execution.getVariable("approvalDecision")).thenReturn(null);
        when(execution.getVariable("rejectionReason")).thenReturn(null);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        specReviewNotificationDelegate.execute(execution);

        verify(execution).setVariable("finalStatus", "AUTO_APPROVED");
        verify(execution).setVariable(eq("lastNotification"), any());
    }

    @Test
    void specReviewNotify_notifyApprovalTask() throws Exception {
        when(execution.getProcessInstanceId()).thenReturn("proc-1");
        when(execution.getCurrentActivityId()).thenReturn("notifyApprovalTask");
        when(execution.getVariable("tenantId")).thenReturn("t1");
        when(execution.getVariable("specId")).thenReturn("spec-1");
        when(execution.getVariable("specTitle")).thenReturn("My Spec");
        when(execution.getVariable("submitterId")).thenReturn("user-1");
        when(execution.getVariable("approvalDecision")).thenReturn("APPROVED");
        when(execution.getVariable("rejectionReason")).thenReturn(null);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        specReviewNotificationDelegate.execute(execution);

        verify(execution).setVariable("finalStatus", "APPROVED");
        verify(execution).setVariable(eq("approvedAt"), any());
    }

    @Test
    void specReviewNotify_notifyRejectionTask() throws Exception {
        when(execution.getProcessInstanceId()).thenReturn("proc-1");
        when(execution.getCurrentActivityId()).thenReturn("notifyRejectionTask");
        when(execution.getVariable("tenantId")).thenReturn("t1");
        when(execution.getVariable("specId")).thenReturn("spec-1");
        when(execution.getVariable("specTitle")).thenReturn("My Spec");
        when(execution.getVariable("submitterId")).thenReturn("user-1");
        when(execution.getVariable("approvalDecision")).thenReturn(null);
        when(execution.getVariable("rejectionReason")).thenReturn("Bad format");
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        specReviewNotificationDelegate.execute(execution);

        verify(execution).setVariable("finalStatus", "REJECTED");
        verify(execution).setVariable(eq("rejectedAt"), any());
    }

    @Test
    void specReviewNotify_unknownActivity() throws Exception {
        when(execution.getProcessInstanceId()).thenReturn("proc-1");
        when(execution.getCurrentActivityId()).thenReturn("unknown");
        when(execution.getVariable("tenantId")).thenReturn("t1");
        when(execution.getVariable("specId")).thenReturn("spec-1");
        when(execution.getVariable("specTitle")).thenReturn(null);
        when(execution.getVariable("submitterId")).thenReturn(null);
        when(execution.getVariable("approvalDecision")).thenReturn(null);
        when(execution.getVariable("rejectionReason")).thenReturn(null);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        specReviewNotificationDelegate.execute(execution);

        verify(execution, never()).setVariable(eq("finalStatus"), any());
    }

    // ========== AiAgentInitDelegate ==========

    @Test
    void aiAgentInit_missingAgentId_throwsException() {
        when(execution.getProcessInstanceId()).thenReturn("proc-1");
        when(execution.getVariable("tenantId")).thenReturn("t1");
        when(execution.getVariable("agentId")).thenReturn(null);

        assertThatThrownBy(() -> aiAgentInitDelegate.execute(execution))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("agentId");
    }

    @Test
    void aiAgentInit_missingTaskDescription_throwsException() {
        when(execution.getProcessInstanceId()).thenReturn("proc-1");
        when(execution.getVariable("tenantId")).thenReturn("t1");
        when(execution.getVariable("agentId")).thenReturn("agent-1");
        when(execution.getVariable("taskDescription")).thenReturn(null);

        assertThatThrownBy(() -> aiAgentInitDelegate.execute(execution))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("taskDescription");
    }

    @Test
    void aiAgentInit_validInput_setsDefaults() {
        when(execution.getProcessInstanceId()).thenReturn("proc-1");
        when(execution.getVariable("tenantId")).thenReturn("t1");
        when(execution.getVariable("agentId")).thenReturn("agent-1");
        when(execution.getVariable("taskDescription")).thenReturn("Do something");
        when(execution.getVariable("trustScore")).thenReturn(null);
        when(execution.getVariable("requireHumanApproval")).thenReturn(null);
        when(execution.getVariable("userId")).thenReturn(null);

        aiAgentInitDelegate.execute(execution);

        verify(execution).setVariable("trustScore", 0.5);
        verify(execution).setVariable("requireHumanApproval", true);
        verify(execution).setVariable("userId", "ai-supervisor");
        verify(execution).setVariable("retryCount", 0);
    }

    @Test
    void aiAgentInit_withNumericTrustScore_parsesCorrectly() {
        when(execution.getProcessInstanceId()).thenReturn("proc-1");
        when(execution.getVariable("tenantId")).thenReturn("t1");
        when(execution.getVariable("agentId")).thenReturn("agent-1");
        when(execution.getVariable("taskDescription")).thenReturn("Do something");
        when(execution.getVariable("trustScore")).thenReturn(0.9);
        when(execution.getVariable("requireHumanApproval")).thenReturn(null);

        aiAgentInitDelegate.execute(execution);

        verify(execution, never()).setVariable(eq("trustScore"), any());
    }

    @Test
    void aiAgentInit_withStringTrustScore_parsesCorrectly() {
        when(execution.getProcessInstanceId()).thenReturn("proc-1");
        when(execution.getVariable("tenantId")).thenReturn("t1");
        when(execution.getVariable("agentId")).thenReturn("agent-1");
        when(execution.getVariable("taskDescription")).thenReturn("Do something");
        when(execution.getVariable("trustScore")).thenReturn("0.75");
        when(execution.getVariable("requireHumanApproval")).thenReturn(null);

        aiAgentInitDelegate.execute(execution);

        verify(execution, never()).setVariable(eq("trustScore"), any());
    }

    @Test
    void aiAgentInit_withInvalidTrustScore_defaultsToHalf() {
        when(execution.getProcessInstanceId()).thenReturn("proc-1");
        when(execution.getVariable("tenantId")).thenReturn("t1");
        when(execution.getVariable("agentId")).thenReturn("agent-1");
        when(execution.getVariable("taskDescription")).thenReturn("Do something");
        when(execution.getVariable("trustScore")).thenReturn("bad");
        when(execution.getVariable("requireHumanApproval")).thenReturn(null);

        aiAgentInitDelegate.execute(execution);

        verify(execution).setVariable("trustScore", 0.5);
    }

    @Test
    void aiAgentInit_highTrustScore_noApprovalRequired() {
        when(execution.getProcessInstanceId()).thenReturn("proc-1");
        when(execution.getVariable("tenantId")).thenReturn("t1");
        when(execution.getVariable("agentId")).thenReturn("agent-1");
        when(execution.getVariable("taskDescription")).thenReturn("Do something");
        when(execution.getVariable("trustScore")).thenReturn(0.9);
        when(execution.getVariable("requireHumanApproval")).thenReturn(null);

        aiAgentInitDelegate.execute(execution);

        verify(execution).setVariable("requireHumanApproval", false);
    }

    @Test
    void aiAgentInit_notify_delegatesToExecute() {
        when(execution.getProcessInstanceId()).thenReturn("proc-1");
        when(execution.getVariable("tenantId")).thenReturn("t1");
        when(execution.getVariable("agentId")).thenReturn("agent-1");
        when(execution.getVariable("taskDescription")).thenReturn("Do something");
        when(execution.getVariable("trustScore")).thenReturn(0.9);
        when(execution.getVariable("requireHumanApproval")).thenReturn(null);
        when(execution.getVariable("userId")).thenReturn(null);

        aiAgentInitDelegate.notify(execution);

        verify(execution).setVariable("userId", "ai-supervisor");
    }

    // ========== AiAgentExecutionDelegate ==========

    @Test
    void aiAgentExecution_successResult() throws Exception {
        when(execution.getProcessInstanceId()).thenReturn("proc-1");
        when(execution.getCurrentActivityId()).thenReturn("act-1");
        when(execution.getVariable("tenantId")).thenReturn("t1");
        when(execution.getVariable("agentId")).thenReturn("agent-1");
        when(execution.getVariable("taskDescription")).thenReturn("task");
        when(execution.getVariable("executionTrackingId")).thenReturn("track-1");
        when(execution.getVariable("retryCount")).thenReturn(null);
        when(execution.getVariable("humanFeedback")).thenReturn(null);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        Map<String, Object> output = new HashMap<>();
        output.put("key1", "val1");
        output.put("key2", "val2");
        NodeExecutionResult result = NodeExecutionResult.success(output);
        when(nodeEngine.executeNode(any())).thenReturn(result);

        aiAgentExecutionDelegate.execute(execution);

        verify(execution).setVariable(eq("agentResult"), any(Map.class));
        verify(execution).setVariable("lastExecutionSuccess", true);
        verify(execution).setVariable("retryCount", 0);
    }

    @Test
    void aiAgentExecution_failureResult() throws Exception {
        when(execution.getProcessInstanceId()).thenReturn("proc-1");
        when(execution.getCurrentActivityId()).thenReturn("act-1");
        when(execution.getVariable("tenantId")).thenReturn("t1");
        when(execution.getVariable("agentId")).thenReturn("agent-1");
        when(execution.getVariable("taskDescription")).thenReturn("task");
        when(execution.getVariable("executionTrackingId")).thenReturn("track-1");
        when(execution.getVariable("retryCount")).thenReturn(1);
        when(execution.getVariable("humanFeedback")).thenReturn(null);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        NodeExecutionResult result = NodeExecutionResult.failure("Something failed");
        when(nodeEngine.executeNode(any())).thenReturn(result);

        aiAgentExecutionDelegate.execute(execution);

        verify(execution).setVariable("lastExecutionSuccess", false);
        verify(execution).setVariable("retryCount", 2);
    }

    @Test
    void aiAgentExecution_withHumanFeedback() throws Exception {
        when(execution.getProcessInstanceId()).thenReturn("proc-1");
        when(execution.getCurrentActivityId()).thenReturn("act-1");
        when(execution.getVariable("tenantId")).thenReturn("t1");
        when(execution.getVariable("agentId")).thenReturn("agent-1");
        when(execution.getVariable("taskDescription")).thenReturn("task");
        when(execution.getVariable("executionTrackingId")).thenReturn("track-1");
        when(execution.getVariable("retryCount")).thenReturn(null);
        when(execution.getVariable("humanFeedback")).thenReturn("Please fix");
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        NodeExecutionResult result = NodeExecutionResult.success(Map.of());
        when(nodeEngine.executeNode(any())).thenReturn(result);

        aiAgentExecutionDelegate.execute(execution);

        verify(execution).setVariable(eq("agentResult"), any(Map.class));
    }

    @Test
    void aiAgentExecution_exception_setsErrorResult() throws Exception {
        when(execution.getProcessInstanceId()).thenReturn("proc-1");
        when(execution.getCurrentActivityId()).thenReturn("act-1");
        when(execution.getVariable("tenantId")).thenReturn("t1");
        when(execution.getVariable("agentId")).thenReturn("agent-1");
        when(execution.getVariable("taskDescription")).thenReturn("task");
        when(execution.getVariable("executionTrackingId")).thenReturn("track-1");
        when(execution.getVariable("retryCount")).thenReturn(null);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        when(nodeEngine.executeNode(any())).thenThrow(new RuntimeException("Boom"));

        assertThatThrownBy(() -> aiAgentExecutionDelegate.execute(execution))
                .isInstanceOf(com.schemaplexai.common.exception.BaseException.class);
    }

    // ========== AiAgentResultProcessorDelegate ==========

    @Test
    void aiAgentResult_processResultTask() throws Exception {
        when(execution.getProcessInstanceId()).thenReturn("proc-1");
        when(execution.getCurrentActivityId()).thenReturn("processResultTask");
        when(execution.getVariable("tenantId")).thenReturn("t1");

        Map<String, Object> agentResult = new HashMap<>();
        agentResult.put("status", "OK");
        when(execution.getVariable("agentResult")).thenReturn(agentResult);
        when(execution.getVariable("executionTrackingId")).thenReturn("track-1");
        when(execution.getVariable("agentId")).thenReturn("agent-1");
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        aiAgentResultProcessorDelegate.execute(execution);

        verify(execution).setVariable(eq("agentResult"), any(Map.class));
        verify(execution).setVariable(eq("executionSummary"), any());
    }

    @Test
    void aiAgentResult_finalizeTask() throws Exception {
        when(execution.getProcessInstanceId()).thenReturn("proc-1");
        when(execution.getCurrentActivityId()).thenReturn("finalizeTask");
        when(execution.getVariable("tenantId")).thenReturn("t1");

        Map<String, Object> agentResult = new HashMap<>();
        agentResult.put("status", "OK");
        when(execution.getVariable("agentResult")).thenReturn(agentResult);
        when(execution.getVariable("executionTrackingId")).thenReturn("track-1");
        when(execution.getVariable("agentId")).thenReturn("agent-1");
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        aiAgentResultProcessorDelegate.execute(execution);

        verify(execution).setVariable("finalStatus", "COMPLETED");
        verify(execution).setVariable(eq("completedAt"), any());
    }

    @Test
    void aiAgentResult_defaultActivity() throws Exception {
        when(execution.getProcessInstanceId()).thenReturn("proc-1");
        when(execution.getCurrentActivityId()).thenReturn("other");
        when(execution.getVariable("tenantId")).thenReturn("t1");
        when(execution.getVariable("agentResult")).thenReturn(null);
        when(execution.getVariable("executionTrackingId")).thenReturn("track-1");
        when(execution.getVariable("agentId")).thenReturn("agent-1");
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        aiAgentResultProcessorDelegate.execute(execution);

        verify(execution, never()).setVariable(eq("finalStatus"), any());
    }

    @Test
    void aiAgentResult_notify_endEvent() {
        when(execution.getEventName()).thenReturn("end");
        when(execution.getProcessInstanceId()).thenReturn("proc-1");
        when(execution.getVariable("tenantId")).thenReturn("t1");
        when(execution.getVariable("finalStatus")).thenReturn(null);

        aiAgentResultProcessorDelegate.notify(execution);

        verify(execution).setVariable("finalStatus", "TERMINATED");
    }

    @Test
    void aiAgentResult_notify_nonEndEvent_doesNothing() {
        when(execution.getEventName()).thenReturn("start");

        aiAgentResultProcessorDelegate.notify(execution);

        verify(execution).getEventName();
        verifyNoMoreInteractions(execution);
    }

    // ========== HumanTaskAssignmentDelegate ==========

    @Test
    void humanTask_createEvent() {
        DelegateTask delegateTask = mock(DelegateTask.class);
        when(delegateTask.getId()).thenReturn("task-1");
        when(delegateTask.getName()).thenReturn("Review");
        when(delegateTask.getProcessInstanceId()).thenReturn("proc-1");
        when(delegateTask.getEventName()).thenReturn("create");
        when(delegateTask.getVariable("tenantId")).thenReturn("t1");
        when(delegateTask.getAssignee()).thenReturn("user-1");
        when(delegateTask.getCandidates()).thenReturn(java.util.Collections.emptySet());

        HumanTaskAssignmentDelegate delegate = new HumanTaskAssignmentDelegate();
        delegate.notify(delegateTask);

        verify(delegateTask).setVariableLocal(eq("assignmentMeta"), any(Map.class));
        verify(delegateTask).setVariable("lastTaskAssigned", "task-1");
    }

    @Test
    void humanTask_assignmentEvent() {
        DelegateTask delegateTask = mock(DelegateTask.class);
        when(delegateTask.getId()).thenReturn("task-1");
        when(delegateTask.getName()).thenReturn("Review");
        when(delegateTask.getProcessInstanceId()).thenReturn("proc-1");
        when(delegateTask.getEventName()).thenReturn("assignment");
        when(delegateTask.getVariable("tenantId")).thenReturn("t1");
        when(delegateTask.getAssignee()).thenReturn("user-2");

        HumanTaskAssignmentDelegate delegate = new HumanTaskAssignmentDelegate();
        delegate.notify(delegateTask);

        verify(delegateTask).setVariableLocal(eq("assignmentMeta"), any(Map.class));
    }

    @Test
    void humanTask_completeEvent() {
        DelegateTask delegateTask = mock(DelegateTask.class);
        when(delegateTask.getId()).thenReturn("task-1");
        when(delegateTask.getName()).thenReturn("Review");
        when(delegateTask.getProcessInstanceId()).thenReturn("proc-1");
        when(delegateTask.getEventName()).thenReturn("complete");
        when(delegateTask.getVariable("tenantId")).thenReturn("t1");

        HumanTaskAssignmentDelegate delegate = new HumanTaskAssignmentDelegate();
        delegate.notify(delegateTask);

        verify(delegateTask).setVariable("lastTaskCompleted", "task-1");
    }

    @Test
    void humanTask_deleteEvent() {
        DelegateTask delegateTask = mock(DelegateTask.class);
        when(delegateTask.getId()).thenReturn("task-1");
        when(delegateTask.getName()).thenReturn("Review");
        when(delegateTask.getProcessInstanceId()).thenReturn("proc-1");
        when(delegateTask.getEventName()).thenReturn("delete");
        when(delegateTask.getVariable("tenantId")).thenReturn("t1");

        HumanTaskAssignmentDelegate delegate = new HumanTaskAssignmentDelegate();
        delegate.notify(delegateTask);

        verify(delegateTask, never()).setVariableLocal(any(), any());
    }

    @Test
    void humanTask_blankTenantId_fallsBack() {
        DelegateTask delegateTask = mock(DelegateTask.class);
        when(delegateTask.getId()).thenReturn("task-1");
        when(delegateTask.getName()).thenReturn("Review");
        when(delegateTask.getProcessInstanceId()).thenReturn("proc-1");
        when(delegateTask.getEventName()).thenReturn("delete");
        when(delegateTask.getVariable("tenantId")).thenReturn("");
        TenantContextHolder.setTenantId("ctx-tenant");

        HumanTaskAssignmentDelegate delegate = new HumanTaskAssignmentDelegate();
        delegate.notify(delegateTask);

        verify(delegateTask).setVariable("tenantId", "ctx-tenant");
    }
}
