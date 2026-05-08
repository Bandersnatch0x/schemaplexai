package com.schemaplexai.agent.engine.approval;

import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.state.AgentExecutionState;
import com.schemaplexai.agent.engine.state.AgentStateMachine;
import com.schemaplexai.agent.engine.tool.ToolCall;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ToolApprovalServiceTest {

    @Mock
    private ApprovalService approvalService;

    @Mock
    private AgentStateMachine stateMachine;

    @Test
    void shouldNotPauseForLowRiskToolInAutoMode() {
        ToolRiskClassifier classifier = new ToolRiskClassifier();
        ToolApprovalService service = new ToolApprovalService(
                approvalService, classifier, ApprovalMode.AUTO, 60);

        SfAgentExecution execution = createExecution(1L);
        ToolCall toolCall = new ToolCall("fileRead");

        boolean result = service.checkAndRequestApproval(toolCall, execution, stateMachine);

        assertFalse(result);
        verifyNoInteractions(approvalService);
        verifyNoInteractions(stateMachine);
    }

    @Test
    void shouldPauseForHighRiskToolInManualMode() {
        ToolRiskClassifier classifier = new ToolRiskClassifier();
        ToolApprovalService service = new ToolApprovalService(
                approvalService, classifier, ApprovalMode.MANUAL, 60);

        SfAgentExecution execution = createExecution(2L);
        ToolCall toolCall = new ToolCall("volumeDelete");

        boolean result = service.checkAndRequestApproval(toolCall, execution, stateMachine);

        assertTrue(result);
        verify(approvalService).requestApproval(any(ApprovalRequest.class));
        verify(stateMachine).transition(AgentExecutionState.PAUSED, execution);
        assertEquals("volumeDelete", execution.getMetadata("pendingApprovalTool"));
    }

    @Test
    void shouldNotPauseForHighRiskToolInAutoMode() {
        ToolRiskClassifier classifier = new ToolRiskClassifier();
        ToolApprovalService service = new ToolApprovalService(
                approvalService, classifier, ApprovalMode.AUTO, 60);

        SfAgentExecution execution = createExecution(3L);
        ToolCall toolCall = new ToolCall("volumeDelete");

        boolean result = service.checkAndRequestApproval(toolCall, execution, stateMachine);

        assertFalse(result);
        verifyNoInteractions(approvalService);
    }

    @Test
    void shouldReturnCorrectApprovalMode() {
        ToolRiskClassifier classifier = new ToolRiskClassifier();
        ToolApprovalService service = new ToolApprovalService(
                approvalService, classifier, ApprovalMode.MANUAL, 60);

        assertEquals(ApprovalMode.MANUAL, service.getApprovalMode());
    }

    @Test
    void shouldDelegateHasPendingApproval() {
        ToolRiskClassifier classifier = new ToolRiskClassifier();
        ToolApprovalService service = new ToolApprovalService(
                approvalService, classifier, ApprovalMode.MANUAL, 60);

        when(approvalService.hasPendingRequest("123")).thenReturn(true);

        assertTrue(service.hasPendingApproval("123"));
        verify(approvalService).hasPendingRequest("123");
    }

    @Test
    void shouldStoreApprovalMetadataOnPause() {
        ToolRiskClassifier classifier = new ToolRiskClassifier();
        ToolApprovalService service = new ToolApprovalService(
                approvalService, classifier, ApprovalMode.MANUAL, 60);

        SfAgentExecution execution = createExecution(4L);
        ToolCall toolCall = new ToolCall("dropDatabase");

        service.checkAndRequestApproval(toolCall, execution, stateMachine);

        assertEquals("dropDatabase", execution.getMetadata("pendingApprovalTool"));
        assertEquals("MANUAL", execution.getMetadata("approvalMode"));
    }

    private SfAgentExecution createExecution(Long id) {
        SfAgentExecution e = new SfAgentExecution();
        e.setId(id);
        e.setAgentId(1L);
        e.setTenantId("tenant-1");
        return e;
    }
}
