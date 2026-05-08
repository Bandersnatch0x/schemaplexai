package com.schemaplexai.agent.engine.state;

import com.schemaplexai.agent.engine.approval.ApprovalMode;
import com.schemaplexai.agent.engine.approval.ToolApprovalService;
import com.schemaplexai.agent.engine.approval.ToolRiskClassifier;
import com.schemaplexai.agent.engine.approval.ApprovalService;
import com.schemaplexai.agent.engine.config.SecurityPolicyLoader;
import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.loop.AgentLoopDetectionService;
import com.schemaplexai.agent.engine.loop.LoopDetectionResult;
import com.schemaplexai.agent.engine.memory.CompositeChatMemoryStore;
import com.schemaplexai.agent.engine.model.LlmMessage;
import com.schemaplexai.agent.engine.tool.*;
import com.schemaplexai.agent.engine.tool.adapter.ExecutionContext;
import com.schemaplexai.agent.engine.tool.adapter.ToolAdapter;
import com.schemaplexai.agent.engine.tool.registry.ToolRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ToolCallingStateHandlerTest {

    @Mock
    private CompositeChatMemoryStore chatMemoryStore;

    @Mock
    private ToolRegistry toolRegistry;

    @Mock
    private ToolSandbox sandbox;

    @Mock
    private ToolSafetyGuard safetyGuard;

    @Mock
    private AgentLoopDetectionService loopDetection;

    @Mock
    private ToolExecutionRecorder executionRecorder;

    @Mock
    private SecurityPolicyLoader securityPolicyLoader;

    @Mock
    private com.schemaplexai.agent.engine.config.AgentEngineProperties engineProperties;

    @Mock
    private AgentStateMachine stateMachine;

    @Mock
    private ToolAdapter toolAdapter;

    @Mock
    private ApprovalService approvalService;

    private ToolApprovalService toolApprovalService;

    @InjectMocks
    private ToolCallingStateHandler handler;

    @BeforeEach
    void setUp() {
        lenient().when(engineProperties.getMaxToolCallsPerIteration()).thenReturn(10);
        // Create a real ToolApprovalService with AUTO mode (no approval needed for tests)
        ToolRiskClassifier riskClassifier = new ToolRiskClassifier();
        toolApprovalService = new ToolApprovalService(approvalService, riskClassifier, ApprovalMode.AUTO, 60);
        handler = new ToolCallingStateHandler(
                chatMemoryStore, sandbox, toolRegistry, safetyGuard,
                loopDetection, executionRecorder, securityPolicyLoader,
                engineProperties, toolApprovalService);
    }

    @Test
    void shouldBlockIrreversibleToolAndTransitionToFailed() {
        when(engineProperties.getMaxToolCalls()).thenReturn(10);
        SfAgentExecution execution = createExecution(1L);
        LlmMessage assistantMsg = new LlmMessage("assistant", "calling volumeDelete");
        when(chatMemoryStore.loadMessages("conv-1")).thenReturn(List.of(assistantMsg));
        when(toolRegistry.parse("calling volumeDelete", null)).thenReturn(List.of(new ToolCall("volumeDelete")));
        when(loopDetection.detectLoop(eq(1L), anyString(), anyList())).thenReturn(LoopDetectionResult.noLoop());
        when(toolRegistry.resolve("volumeDelete")).thenReturn(toolAdapter);
        when(securityPolicyLoader.load("tenant-1")).thenReturn(null);

        ToolSafetyGuard.SafetyCheckResult blockResult = new ToolSafetyGuard.SafetyCheckResult(
            false, true, ToolErrorCategory.IRREVERSIBLE_OPERATION, "Irreversible"
        );
        when(safetyGuard.check("volumeDelete", "{}", "tenant-1")).thenReturn(blockResult);

        handler.handle(stateMachine, execution);

        verify(executionRecorder).record(eq(1L), argThat(result ->
            result.blocked() && result.errorCategory() == ToolErrorCategory.IRREVERSIBLE_OPERATION));
        verify(stateMachine).transition(AgentExecutionState.GATE_BLOCKED, execution);
    }

    @Test
    void shouldExecuteSafeToolAndRecordSuccess() {
        when(engineProperties.getMaxToolCalls()).thenReturn(10);
        SfAgentExecution execution = createExecution(2L);
        LlmMessage assistantMsg = new LlmMessage("assistant", "calling fileRead");
        when(chatMemoryStore.loadMessages("conv-2")).thenReturn(List.of(assistantMsg));
        when(toolRegistry.parse("calling fileRead", null)).thenReturn(List.of(new ToolCall("fileRead")));
        when(loopDetection.detectLoop(eq(2L), anyString(), anyList())).thenReturn(LoopDetectionResult.noLoop());
        when(toolRegistry.resolve("fileRead")).thenReturn(toolAdapter);
        when(securityPolicyLoader.load("tenant-2")).thenReturn(null);
        when(safetyGuard.check("fileRead", "{}", "tenant-2")).thenReturn(
            new ToolSafetyGuard.SafetyCheckResult(true, false, null, null));
        try {
            when(toolAdapter.execute(any(ToolCall.class), any(ExecutionContext.class)))
                .thenReturn(ToolResult.success("Tool fileRead executed"));
        } catch (ToolExecutionException e) {
            throw new RuntimeException(e);
        }

        handler.handle(stateMachine, execution);

        verify(executionRecorder).record(eq(2L), argThat(ToolExecutionResult::success));
        verify(chatMemoryStore).saveMessage(eq("conv-2"), argThat(msg ->
            msg.getRole().equals("tool") && msg.getContent().contains("Tool fileRead executed")));
        verify(stateMachine).transition(AgentExecutionState.THINKING, execution);
    }

    @Test
    void shouldTransitionToCompletedWhenNoMessages() {
        SfAgentExecution execution = createExecution(3L);
        when(chatMemoryStore.loadMessages("conv-3")).thenReturn(List.of());

        handler.handle(stateMachine, execution);

        verify(stateMachine).transition(AgentExecutionState.COMPLETED, execution);
        verifyNoInteractions(safetyGuard);
    }

    @Test
    void shouldTransitionToCompletedWhenLastMessageNotAssistant() {
        SfAgentExecution execution = createExecution(4L);
        LlmMessage userMsg = new LlmMessage("user", "Hello");
        when(chatMemoryStore.loadMessages("conv-4")).thenReturn(List.of(userMsg));

        handler.handle(stateMachine, execution);

        verify(stateMachine).transition(AgentExecutionState.COMPLETED, execution);
        verifyNoInteractions(safetyGuard);
    }

    @Test
    void shouldHandleExceptionAndTransitionToFailed() {
        SfAgentExecution execution = createExecution(5L);
        when(chatMemoryStore.loadMessages("conv-5")).thenThrow(new RuntimeException("DB error"));

        handler.handle(stateMachine, execution);

        verify(executionRecorder).record(eq(5L), argThat(result ->
            !result.success() && result.errorCategory() == ToolErrorCategory.UNEXPECTED_ENVIRONMENT));
        verify(stateMachine).transition(AgentExecutionState.FAILED, execution);
    }

    @Test
    void shouldHandleToolExecutionFailureAndRecordIt() {
        when(engineProperties.getMaxToolCalls()).thenReturn(10);
        SfAgentExecution execution = createExecution(6L);
        LlmMessage assistantMsg = new LlmMessage("assistant", "calling failStub");
        when(chatMemoryStore.loadMessages("conv-6")).thenReturn(List.of(assistantMsg));
        when(toolRegistry.parse("calling failStub", null)).thenReturn(List.of(new ToolCall("failStub")));
        when(loopDetection.detectLoop(eq(6L), anyString(), anyList())).thenReturn(LoopDetectionResult.noLoop());
        when(toolRegistry.resolve("failStub")).thenReturn(null);

        handler.handle(stateMachine, execution);

        verify(executionRecorder).record(eq(6L), argThat(result ->
            !result.success() && result.errorCategory() == ToolErrorCategory.INVALID_ARGUMENT));
        verify(stateMachine).transition(AgentExecutionState.FAILED, execution);
    }

    @Test
    void shouldBlockWhenToolCallBudgetExceeded() {
        when(engineProperties.getMaxToolCalls()).thenReturn(3);
        SfAgentExecution execution = createExecution(7L);
        execution.setMetadata("toolCallCount", 3);

        LlmMessage assistantMsg = new LlmMessage("assistant", "calling fileRead");
        when(chatMemoryStore.loadMessages("conv-7")).thenReturn(List.of(assistantMsg));
        when(toolRegistry.parse("calling fileRead", null)).thenReturn(List.of(new ToolCall("fileRead")));

        handler.handle(stateMachine, execution);

        verify(stateMachine).transition(AgentExecutionState.GATE_BLOCKED, execution);
        assertEquals("tool_call_budget_exceeded", execution.getMetadata("blockedReason"));
        assertEquals("BUDGET", execution.getMetadata("admissionType"));
        verifyNoInteractions(toolAdapter);
    }

    @Test
    void shouldIncrementToolCallCountOnSuccess() throws ToolExecutionException {
        when(engineProperties.getMaxToolCalls()).thenReturn(10);
        SfAgentExecution execution = createExecution(8L);
        execution.setMetadata("toolCallCount", 2);

        LlmMessage assistantMsg = new LlmMessage("assistant", "calling fileRead");
        when(chatMemoryStore.loadMessages("conv-8")).thenReturn(List.of(assistantMsg));
        when(toolRegistry.parse("calling fileRead", null)).thenReturn(List.of(new ToolCall("fileRead")));
        when(loopDetection.detectLoop(eq(8L), anyString(), anyList())).thenReturn(LoopDetectionResult.noLoop());
        when(toolRegistry.resolve("fileRead")).thenReturn(toolAdapter);
        when(securityPolicyLoader.load("tenant-8")).thenReturn(null);
        when(safetyGuard.check("fileRead", "{}", "tenant-8")).thenReturn(
            new ToolSafetyGuard.SafetyCheckResult(true, false, null, null));
        when(toolAdapter.execute(any(ToolCall.class), any(ExecutionContext.class)))
            .thenReturn(ToolResult.success("Tool fileRead executed"));

        handler.handle(stateMachine, execution);

        assertEquals(3, execution.getMetadata("toolCallCount"));
        verify(stateMachine).transition(AgentExecutionState.THINKING, execution);
    }

    @Test
    void getStateShouldReturnToolCalling() {
        assertEquals(AgentExecutionState.TOOL_CALLING, handler.getState());
    }

    // ------------------------------------------------------------------
    // Edge-case tests (Task #15)
    // ------------------------------------------------------------------

    @Test
    void shouldTransitionToCompletedWhenEmptyToolCallsList() {
        SfAgentExecution execution = createExecution(20L);
        LlmMessage assistantMsg = new LlmMessage("assistant", "No tools here");
        when(chatMemoryStore.loadMessages("conv-20")).thenReturn(List.of(assistantMsg));
        when(toolRegistry.parse("No tools here", null)).thenReturn(List.of());

        handler.handle(stateMachine, execution);

        verify(stateMachine).transition(AgentExecutionState.THINKING, execution);
        verifyNoInteractions(safetyGuard, toolAdapter, executionRecorder, loopDetection);
    }

    @Test
    void shouldBlockAllToolsWhenSafetyGuardBlocksEverything() {
        when(engineProperties.getMaxToolCalls()).thenReturn(10);
        SfAgentExecution execution = createExecution(21L);
        LlmMessage assistantMsg = new LlmMessage("assistant", "calling toolA and toolB");
        when(chatMemoryStore.loadMessages("conv-21")).thenReturn(List.of(assistantMsg));

        ToolCall toolA = new ToolCall("toolA");
        ToolCall toolB = new ToolCall("toolB");
        when(toolRegistry.parse("calling toolA and toolB", null)).thenReturn(List.of(toolA, toolB));
        when(loopDetection.detectLoop(eq(21L), anyString(), anyList())).thenReturn(LoopDetectionResult.noLoop());
        when(toolRegistry.resolve("toolA")).thenReturn(toolAdapter);
        when(securityPolicyLoader.load("tenant-21")).thenReturn(null);

        ToolSafetyGuard.SafetyCheckResult blockResult = new ToolSafetyGuard.SafetyCheckResult(
            false, true, ToolErrorCategory.IRREVERSIBLE_OPERATION, "Blocked by policy"
        );
        when(safetyGuard.check("toolA", "{}", "tenant-21")).thenReturn(blockResult);

        handler.handle(stateMachine, execution);

        verify(executionRecorder).record(eq(21L), argThat(result ->
            result.blocked() && result.errorCategory() == ToolErrorCategory.IRREVERSIBLE_OPERATION));
        verify(stateMachine).transition(AgentExecutionState.GATE_BLOCKED, execution);
        try {
            verify(toolAdapter, never()).execute(any(), any()); // ToolExecutionException is checked but never() doesn't actually call it
        } catch (ToolExecutionException e) {
            throw new RuntimeException(e);
        }
        // toolB should never be reached because toolA blocks and we return early
        verify(toolRegistry, never()).resolve("toolB");
    }

    @Test
    void shouldHandleToolAdapterThrowsToolExecutionException() throws ToolExecutionException {
        when(engineProperties.getMaxToolCalls()).thenReturn(10);
        SfAgentExecution execution = createExecution(22L);
        LlmMessage assistantMsg = new LlmMessage("assistant", "calling flakyTool");
        when(chatMemoryStore.loadMessages("conv-22")).thenReturn(List.of(assistantMsg));
        when(toolRegistry.parse("calling flakyTool", null)).thenReturn(List.of(new ToolCall("flakyTool")));
        when(loopDetection.detectLoop(eq(22L), anyString(), anyList())).thenReturn(LoopDetectionResult.noLoop());
        when(toolRegistry.resolve("flakyTool")).thenReturn(toolAdapter);
        when(securityPolicyLoader.load("tenant-22")).thenReturn(null);
        when(safetyGuard.check("flakyTool", "{}", "tenant-22")).thenReturn(
            new ToolSafetyGuard.SafetyCheckResult(true, false, null, null));
        when(toolAdapter.execute(any(ToolCall.class), any(ExecutionContext.class)))
            .thenThrow(new ToolExecutionException(ToolErrorCategory.TIMEOUT, "Connection timed out"));

        handler.handle(stateMachine, execution);

        verify(executionRecorder).record(eq(22L), argThat(result ->
            !result.success()
                && result.errorCategory() == ToolErrorCategory.TIMEOUT
                && result.errorMessage().contains("Connection timed out")));
        verify(stateMachine).transition(AgentExecutionState.RETRYING, execution);
        assertEquals("TIMEOUT", execution.getMetadata("lastErrorCategory"));
    }

    @Test
    void shouldTransitionToFailedWhenRetryExhaustedOnNonRetryableError() throws ToolExecutionException {
        when(engineProperties.getMaxToolCalls()).thenReturn(10);
        SfAgentExecution execution = createExecution(23L);
        execution.setMetadata("retryContext", "previous-attempt");
        execution.setMetadata("failedToolName", "badTool");

        LlmMessage assistantMsg = new LlmMessage("assistant", "calling badTool");
        when(chatMemoryStore.loadMessages("conv-23")).thenReturn(List.of(assistantMsg));
        when(toolRegistry.parse("calling badTool", null)).thenReturn(List.of(new ToolCall("badTool")));
        when(loopDetection.detectLoop(eq(23L), anyString(), anyList())).thenReturn(LoopDetectionResult.noLoop());
        when(toolRegistry.resolve("badTool")).thenReturn(toolAdapter);
        when(securityPolicyLoader.load("tenant-23")).thenReturn(null);
        when(safetyGuard.check("badTool", "{}", "tenant-23")).thenReturn(
            new ToolSafetyGuard.SafetyCheckResult(true, false, null, null));
        // INVALID_ARGUMENT is not retryable
        when(toolAdapter.execute(any(ToolCall.class), any(ExecutionContext.class)))
            .thenThrow(new ToolExecutionException(ToolErrorCategory.INVALID_ARGUMENT, "Missing required param"));

        handler.handle(stateMachine, execution);

        verify(executionRecorder).record(eq(23L), argThat(result ->
            !result.success()
                && result.errorCategory() == ToolErrorCategory.INVALID_ARGUMENT));
        verify(stateMachine).transition(AgentExecutionState.FAILED, execution);
        assertEquals("INVALID_ARGUMENT", execution.getMetadata("lastErrorCategory"));
    }

    @Test
    void shouldBlockWhenBudgetExceededMidExecution() {
        when(engineProperties.getMaxToolCalls()).thenReturn(3);
        SfAgentExecution execution = createExecution(24L);
        execution.setMetadata("toolCallCount", 2);

        LlmMessage assistantMsg = new LlmMessage("assistant", "calling tool1 and tool2");
        when(chatMemoryStore.loadMessages("conv-24")).thenReturn(List.of(assistantMsg));
        // Two tool calls pending but only 1 budget remaining (3 - 2 = 1)
        when(toolRegistry.parse("calling tool1 and tool2", null))
            .thenReturn(List.of(new ToolCall("tool1"), new ToolCall("tool2")));

        handler.handle(stateMachine, execution);

        verify(stateMachine).transition(AgentExecutionState.GATE_BLOCKED, execution);
        assertEquals("tool_call_budget_exceeded", execution.getMetadata("blockedReason"));
        assertEquals("BUDGET", execution.getMetadata("admissionType"));
        verifyNoInteractions(toolAdapter);
    }

    @Test
    void shouldTransitionToCompletedWhenLastMessageIsNonAssistant() {
        SfAgentExecution execution = createExecution(25L);
        LlmMessage systemMsg = new LlmMessage("system", "You are a helpful assistant");
        when(chatMemoryStore.loadMessages("conv-25")).thenReturn(List.of(systemMsg));

        handler.handle(stateMachine, execution);

        verify(stateMachine).transition(AgentExecutionState.COMPLETED, execution);
        verifyNoInteractions(toolRegistry, safetyGuard, toolAdapter);
    }

    @Test
    void shouldSkipNonRetryToolsInRetryContext() throws ToolExecutionException {
        when(engineProperties.getMaxToolCalls()).thenReturn(10);
        SfAgentExecution execution = createExecution(26L);
        execution.setMetadata("retryContext", "attempt-2");
        execution.setMetadata("failedToolName", "targetTool");

        LlmMessage assistantMsg = new LlmMessage("assistant", "calling targetTool and otherTool");
        when(chatMemoryStore.loadMessages("conv-26")).thenReturn(List.of(assistantMsg));

        ToolCall targetTool = new ToolCall("targetTool");
        ToolCall otherTool = new ToolCall("otherTool");
        when(toolRegistry.parse("calling targetTool and otherTool", null))
            .thenReturn(List.of(targetTool, otherTool));
        when(loopDetection.detectLoop(eq(26L), anyString(), anyList())).thenReturn(LoopDetectionResult.noLoop());
        when(toolRegistry.resolve("targetTool")).thenReturn(toolAdapter);
        when(securityPolicyLoader.load("tenant-26")).thenReturn(null);
        when(safetyGuard.check("targetTool", "{}", "tenant-26")).thenReturn(
            new ToolSafetyGuard.SafetyCheckResult(true, false, null, null));
        when(toolAdapter.execute(any(ToolCall.class), any(ExecutionContext.class)))
            .thenReturn(ToolResult.success("Target tool success"));

        handler.handle(stateMachine, execution);

        // otherTool should be skipped in retry context
        verify(toolRegistry, never()).resolve("otherTool");
        verify(toolAdapter).execute(argThat(call -> "targetTool".equals(call.toolName())), any(ExecutionContext.class));
        verify(chatMemoryStore).saveMessage(eq("conv-26"), argThat(msg ->
            msg.getRole().equals("tool") && msg.getContent().contains("Target tool success")));
        verify(stateMachine).transition(AgentExecutionState.THINKING, execution);
    }

    @Test
    void shouldHandleLoopDetectionAndTransitionToGateBlocked() {
        when(engineProperties.getMaxToolCalls()).thenReturn(10);
        SfAgentExecution execution = createExecution(27L);
        LlmMessage assistantMsg = new LlmMessage("assistant", "calling loopTool");
        when(chatMemoryStore.loadMessages("conv-27")).thenReturn(List.of(assistantMsg));
        when(toolRegistry.parse("calling loopTool", null)).thenReturn(List.of(new ToolCall("loopTool")));
        when(loopDetection.detectLoop(eq(27L), anyString(), anyList()))
            .thenReturn(LoopDetectionResult.hashLoop());

        handler.handle(stateMachine, execution);

        verify(stateMachine).transition(AgentExecutionState.GATE_BLOCKED, execution);
        verifyNoInteractions(safetyGuard, toolAdapter);
    }

    @Test
    void shouldHandleAdapterExecuteThrowsRuntimeException() throws ToolExecutionException {
        when(engineProperties.getMaxToolCalls()).thenReturn(10);
        SfAgentExecution execution = createExecution(28L);
        LlmMessage assistantMsg = new LlmMessage("assistant", "calling buggyTool");
        when(chatMemoryStore.loadMessages("conv-28")).thenReturn(List.of(assistantMsg));
        when(toolRegistry.parse("calling buggyTool", null)).thenReturn(List.of(new ToolCall("buggyTool")));
        when(loopDetection.detectLoop(eq(28L), anyString(), anyList())).thenReturn(LoopDetectionResult.noLoop());
        when(toolRegistry.resolve("buggyTool")).thenReturn(toolAdapter);
        when(securityPolicyLoader.load("tenant-28")).thenReturn(null);
        when(safetyGuard.check("buggyTool", "{}", "tenant-28")).thenReturn(
            new ToolSafetyGuard.SafetyCheckResult(true, false, null, null));
        when(toolAdapter.execute(any(ToolCall.class), any(ExecutionContext.class)))
            .thenThrow(new RuntimeException("Unexpected NPE in adapter"));

        handler.handle(stateMachine, execution);

        verify(executionRecorder).record(eq(28L), argThat(result ->
            !result.success()
                && result.errorCategory() == ToolErrorCategory.INTERNAL_ERROR
                && result.errorMessage().contains("Unexpected NPE in adapter")));
        verify(stateMachine).transition(AgentExecutionState.RETRYING, execution);
    }

    @Test
    void shouldResolveToolWithTenantSecurityPolicyEnvironmentOverride() throws ToolExecutionException {
        when(engineProperties.getMaxToolCalls()).thenReturn(10);
        SfAgentExecution execution = createExecution(29L);
        LlmMessage assistantMsg = new LlmMessage("assistant", "calling envTool");
        when(chatMemoryStore.loadMessages("conv-29")).thenReturn(List.of(assistantMsg));
        when(toolRegistry.parse("calling envTool", null)).thenReturn(List.of(new ToolCall("envTool")));
        when(loopDetection.detectLoop(eq(29L), anyString(), anyList())).thenReturn(LoopDetectionResult.noLoop());
        when(toolRegistry.resolve("envTool")).thenReturn(toolAdapter);

        // Security policy overrides environment
        com.schemaplexai.model.entity.config.TenantEnvironmentConfig config =
            new com.schemaplexai.model.entity.config.TenantEnvironmentConfig();
        config.setEnvironment("production");
        when(securityPolicyLoader.load("tenant-29")).thenReturn(config);

        // Safety guard should be called with overridden environment "production"
        when(safetyGuard.check("envTool", "{}", "production")).thenReturn(
            new ToolSafetyGuard.SafetyCheckResult(true, false, null, null));
        when(toolAdapter.execute(any(ToolCall.class), any(ExecutionContext.class)))
            .thenReturn(ToolResult.success("Env tool success"));

        handler.handle(stateMachine, execution);

        verify(safetyGuard).check("envTool", "{}", "production");
        verify(stateMachine).transition(AgentExecutionState.THINKING, execution);
    }

    @Test
    void shouldHandleMissingToolRegistration() {
        when(engineProperties.getMaxToolCalls()).thenReturn(10);
        SfAgentExecution execution = createExecution(30L);
        LlmMessage assistantMsg = new LlmMessage("assistant", "calling unregisteredTool");
        when(chatMemoryStore.loadMessages("conv-30")).thenReturn(List.of(assistantMsg));
        when(toolRegistry.parse("calling unregisteredTool", null))
            .thenReturn(List.of(new ToolCall("unregisteredTool")));
        when(loopDetection.detectLoop(eq(30L), anyString(), anyList())).thenReturn(LoopDetectionResult.noLoop());
        when(toolRegistry.resolve("unregisteredTool")).thenReturn(null);

        handler.handle(stateMachine, execution);

        verify(executionRecorder).record(eq(30L), argThat(result ->
            !result.success()
                && result.errorCategory() == ToolErrorCategory.INVALID_ARGUMENT
                && result.errorMessage().contains("Tool not registered")));
        verify(stateMachine).transition(AgentExecutionState.FAILED, execution);
    }

    @Test
    void shouldBlockWhenPerIterationToolCallBudgetExceeded() {
        when(engineProperties.getMaxToolCalls()).thenReturn(10);
        when(engineProperties.getMaxToolCallsPerIteration()).thenReturn(2);
        SfAgentExecution execution = createExecution(31L);
        execution.setMetadata("iterationToolCallCount", 2);

        LlmMessage assistantMsg = new LlmMessage("assistant", "calling fileRead");
        when(chatMemoryStore.loadMessages("conv-31")).thenReturn(List.of(assistantMsg));
        when(toolRegistry.parse("calling fileRead", null)).thenReturn(List.of(new ToolCall("fileRead")));

        handler.handle(stateMachine, execution);

        verify(stateMachine).transition(AgentExecutionState.GATE_BLOCKED, execution);
        assertEquals("tool_call_per_iteration_budget_exceeded", execution.getMetadata("blockedReason"));
        assertEquals("BUDGET", execution.getMetadata("admissionType"));
        verifyNoInteractions(toolAdapter);
    }

    @Test
    void shouldBlockWhenPendingToolCallsExceedPerIterationBudget() {
        when(engineProperties.getMaxToolCalls()).thenReturn(10);
        when(engineProperties.getMaxToolCallsPerIteration()).thenReturn(2);
        SfAgentExecution execution = createExecution(32L);
        execution.setMetadata("iterationToolCallCount", 1);

        LlmMessage assistantMsg = new LlmMessage("assistant", "calling tool1 and tool2");
        when(chatMemoryStore.loadMessages("conv-32")).thenReturn(List.of(assistantMsg));
        // 1 already used + 2 pending = 3 > 2 max per iteration
        when(toolRegistry.parse("calling tool1 and tool2", null))
            .thenReturn(List.of(new ToolCall("tool1"), new ToolCall("tool2")));

        handler.handle(stateMachine, execution);

        verify(stateMachine).transition(AgentExecutionState.GATE_BLOCKED, execution);
        assertEquals("tool_call_per_iteration_budget_exceeded", execution.getMetadata("blockedReason"));
        verifyNoInteractions(toolAdapter);
    }

    @Test
    void shouldIncrementIterationToolCallCountOnSuccess() throws ToolExecutionException {
        when(engineProperties.getMaxToolCalls()).thenReturn(10);
        when(engineProperties.getMaxToolCallsPerIteration()).thenReturn(5);
        SfAgentExecution execution = createExecution(33L);
        execution.setMetadata("iterationToolCallCount", 1);

        LlmMessage assistantMsg = new LlmMessage("assistant", "calling fileRead");
        when(chatMemoryStore.loadMessages("conv-33")).thenReturn(List.of(assistantMsg));
        when(toolRegistry.parse("calling fileRead", null)).thenReturn(List.of(new ToolCall("fileRead")));
        when(loopDetection.detectLoop(eq(33L), anyString(), anyList())).thenReturn(LoopDetectionResult.noLoop());
        when(toolRegistry.resolve("fileRead")).thenReturn(toolAdapter);
        when(securityPolicyLoader.load("tenant-33")).thenReturn(null);
        when(safetyGuard.check("fileRead", "{}", "tenant-33")).thenReturn(
            new ToolSafetyGuard.SafetyCheckResult(true, false, null, null));
        when(toolAdapter.execute(any(ToolCall.class), any(ExecutionContext.class)))
            .thenReturn(ToolResult.success("Tool fileRead executed"));

        handler.handle(stateMachine, execution);

        assertEquals(2, execution.getMetadata("iterationToolCallCount"));
        assertEquals(1, execution.getMetadata("toolCallCount"));
        verify(stateMachine).transition(AgentExecutionState.THINKING, execution);
    }

    @Test
    void shouldAllowToolCallsWhenIterationBudgetNotExceeded() throws ToolExecutionException {
        when(engineProperties.getMaxToolCalls()).thenReturn(10);
        when(engineProperties.getMaxToolCallsPerIteration()).thenReturn(3);
        SfAgentExecution execution = createExecution(34L);
        execution.setMetadata("iterationToolCallCount", 0);

        LlmMessage assistantMsg = new LlmMessage("assistant", "calling tool1 tool2");
        when(chatMemoryStore.loadMessages("conv-34")).thenReturn(List.of(assistantMsg));
        when(toolRegistry.parse("calling tool1 tool2", null))
            .thenReturn(List.of(new ToolCall("tool1"), new ToolCall("tool2")));
        when(loopDetection.detectLoop(eq(34L), anyString(), anyList())).thenReturn(LoopDetectionResult.noLoop());
        when(toolRegistry.resolve("tool1")).thenReturn(toolAdapter);
        when(toolRegistry.resolve("tool2")).thenReturn(toolAdapter);
        when(securityPolicyLoader.load("tenant-34")).thenReturn(null);
        when(safetyGuard.check(anyString(), any(), eq("tenant-34"))).thenReturn(
            new ToolSafetyGuard.SafetyCheckResult(true, false, null, null));
        when(toolAdapter.execute(any(ToolCall.class), any(ExecutionContext.class)))
            .thenReturn(ToolResult.success("ok"));

        handler.handle(stateMachine, execution);

        assertEquals(2, execution.getMetadata("iterationToolCallCount"));
        assertEquals(2, execution.getMetadata("toolCallCount"));
        verify(stateMachine).transition(AgentExecutionState.THINKING, execution);
    }

    private SfAgentExecution createExecution(Long id) {
        SfAgentExecution e = new SfAgentExecution();
        e.setId(id);
        e.setAgentId(1L);
        e.setConversationId("conv-" + id);
        e.setTenantId("tenant-" + id);
        e.setState(AgentExecutionState.TOOL_CALLING.name());
        return e;
    }
}
