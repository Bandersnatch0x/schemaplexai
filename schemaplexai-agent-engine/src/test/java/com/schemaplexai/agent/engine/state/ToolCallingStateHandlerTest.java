package com.schemaplexai.agent.engine.state;

import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.memory.CompositeChatMemoryStore;
import com.schemaplexai.agent.engine.model.LlmMessage;
import com.schemaplexai.agent.engine.tool.ToolErrorCategory;
import com.schemaplexai.agent.engine.tool.ToolExecutionRecorder;
import com.schemaplexai.agent.engine.tool.ToolExecutionResult;
import com.schemaplexai.agent.engine.tool.ToolSafetyGuard;
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
    private ToolSafetyGuard safetyGuard;

    @Mock
    private ToolExecutionRecorder executionRecorder;

    @Mock
    private AgentStateMachine stateMachine;

    @InjectMocks
    private ToolCallingStateHandler handler;

    @Test
    void shouldBlockIrreversibleToolAndTransitionToFailed() {
        SfAgentExecution execution = createExecution(1L);
        LlmMessage assistantMsg = new LlmMessage("assistant", "calling volumeDelete");
        when(chatMemoryStore.loadMessages("conv-1")).thenReturn(List.of(assistantMsg));

        ToolSafetyGuard.SafetyCheckResult blockResult = new ToolSafetyGuard.SafetyCheckResult(
            false, true, ToolErrorCategory.UNAUTHORIZED_SCOPE, "Irreversible"
        );
        when(safetyGuard.check("volumeDelete", "calling volumeDelete")).thenReturn(blockResult);

        handler.handle(stateMachine, execution);

        verify(executionRecorder).record(eq(1L), argThat(result ->
            result.blocked() && result.errorCategory() == ToolErrorCategory.UNAUTHORIZED_SCOPE));
        verify(chatMemoryStore).saveMessage(eq("conv-1"), argThat(msg ->
            msg.getRole().equals("tool") && msg.getContent().startsWith("BLOCKED:")));
        verify(stateMachine).transition(AgentExecutionState.FAILED, execution);
    }

    @Test
    void shouldExecuteSafeToolAndRecordSuccess() {
        SfAgentExecution execution = createExecution(2L);
        LlmMessage assistantMsg = new LlmMessage("assistant", "calling fileRead");
        when(chatMemoryStore.loadMessages("conv-2")).thenReturn(List.of(assistantMsg));
        when(safetyGuard.check("fileRead", "calling fileRead")).thenReturn(
            new ToolSafetyGuard.SafetyCheckResult(true, false, null, null));

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
        verifyNoInteractions(executionRecorder);
    }

    @Test
    void shouldTransitionToCompletedWhenLastMessageNotAssistant() {
        SfAgentExecution execution = createExecution(4L);
        LlmMessage userMsg = new LlmMessage("user", "Hello");
        when(chatMemoryStore.loadMessages("conv-4")).thenReturn(List.of(userMsg));

        handler.handle(stateMachine, execution);

        verify(stateMachine).transition(AgentExecutionState.COMPLETED, execution);
        verifyNoInteractions(safetyGuard);
        verifyNoInteractions(executionRecorder);
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
    void getStateShouldReturnToolCalling() {
        assertEquals(AgentExecutionState.TOOL_CALLING, handler.getState());
    }

    private SfAgentExecution createExecution(Long id) {
        SfAgentExecution e = new SfAgentExecution();
        e.setId(id);
        e.setAgentId(1L);
        e.setConversationId("conv-" + id);
        e.setState(AgentExecutionState.TOOL_CALLING.name());
        return e;
    }
}
