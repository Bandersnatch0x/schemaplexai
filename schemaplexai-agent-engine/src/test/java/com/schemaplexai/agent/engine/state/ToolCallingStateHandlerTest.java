package com.schemaplexai.agent.engine.state;

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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
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

    @InjectMocks
    private ToolCallingStateHandler handler;

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
        SfAgentExecution execution = createExecution(6L);
        LlmMessage assistantMsg = new LlmMessage("assistant", "calling failStub");
        when(chatMemoryStore.loadMessages("conv-6")).thenReturn(List.of(assistantMsg));

        handler.handle(stateMachine, execution);

        verify(executionRecorder).record(eq(6L), argThat(result ->
            !result.success() && result.errorCategory() == ToolErrorCategory.UNEXPECTED_ENVIRONMENT));
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
