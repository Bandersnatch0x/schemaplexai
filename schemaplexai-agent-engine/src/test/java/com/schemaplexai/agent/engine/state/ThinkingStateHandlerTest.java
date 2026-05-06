package com.schemaplexai.agent.engine.state;

import com.schemaplexai.agent.engine.context.ContextInjector;
import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.loop.AgentLoopDetectionService;
import com.schemaplexai.agent.engine.loop.LoopDetectionResult;
import com.schemaplexai.agent.engine.memory.CompositeChatMemoryStore;
import com.schemaplexai.agent.engine.model.AiModelRouter;
import com.schemaplexai.agent.engine.model.LlmMessage;
import com.schemaplexai.agent.engine.model.ModelResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ThinkingStateHandlerTest {

    @Mock
    private ContextInjector contextInjector;

    @Mock
    private CompositeChatMemoryStore chatMemoryStore;

    @Mock
    private AiModelRouter modelRouter;

    @Mock
    private AgentLoopDetectionService loopDetection;

    @Mock
    private ModelResolver modelResolver;

    @Mock
    private AgentStateMachine stateMachine;

    @InjectMocks
    private ThinkingStateHandler thinkingStateHandler;

    private SfAgentExecution execution;

    @BeforeEach
    void setUp() {
        execution = new SfAgentExecution();
        execution.setId(1L);
        execution.setAgentId(42L);
        execution.setConversationId("conv-123");
        execution.setTokenBudgetJson("32000,4096,0,0");
    }

    @Test
    void getStateShouldReturnThinking() {
        assertEquals(AgentExecutionState.THINKING, thinkingStateHandler.getState());
    }

    @Test
    void handleShouldLoadMessagesFromMemory() {
        when(chatMemoryStore.loadMessages("conv-123")).thenReturn(List.of());
        when(modelResolver.resolve(execution)).thenReturn("gpt-4");
        when(modelRouter.generateWithFallback(anyString(), anyString(), anyDouble()))
                .thenReturn("Direct answer without tools");

        thinkingStateHandler.handle(stateMachine, execution);

        verify(chatMemoryStore, times(1)).loadMessages("conv-123");
    }

    @Test
    void handleShouldInjectContext() {
        List<LlmMessage> messages = new ArrayList<>();
        when(chatMemoryStore.loadMessages("conv-123")).thenReturn(messages);
        when(modelResolver.resolve(execution)).thenReturn("gpt-4");
        when(modelRouter.generateWithFallback(anyString(), anyString(), anyDouble()))
                .thenReturn("Direct answer");

        thinkingStateHandler.handle(stateMachine, execution);

        verify(contextInjector, times(1)).inject(messages, 42L);
    }

    @Test
    void handleShouldCallLlmWithResolvedModel() {
        List<LlmMessage> messages = List.of(new LlmMessage("user", "Hello"));
        when(chatMemoryStore.loadMessages("conv-123")).thenReturn(messages);
        when(modelResolver.resolve(execution)).thenReturn("gpt-4o");
        when(modelRouter.generateWithFallback(anyString(), eq("gpt-4o"), eq(0.7)))
                .thenReturn("Hello! How can I help?");

        thinkingStateHandler.handle(stateMachine, execution);

        verify(modelRouter, times(1)).generateWithFallback(anyString(), eq("gpt-4o"), eq(0.7));
        verify(modelResolver, times(1)).resolve(execution);
    }

    @Test
    void handleShouldSaveAssistantResponseToMemory() {
        when(chatMemoryStore.loadMessages("conv-123")).thenReturn(List.of());
        when(modelResolver.resolve(execution)).thenReturn("gpt-4");
        when(modelRouter.generateWithFallback(anyString(), anyString(), anyDouble()))
                .thenReturn("Assistant response");

        thinkingStateHandler.handle(stateMachine, execution);

        ArgumentCaptor<LlmMessage> messageCaptor = ArgumentCaptor.forClass(LlmMessage.class);
        verify(chatMemoryStore, times(1)).saveMessage(eq("conv-123"), messageCaptor.capture());
        assertEquals("assistant", messageCaptor.getValue().getRole());
        assertEquals("Assistant response", messageCaptor.getValue().getContent());
    }

    @Test
    void handleShouldTransitionToCompletedForDirectAnswer() {
        when(chatMemoryStore.loadMessages("conv-123")).thenReturn(List.of());
        when(modelResolver.resolve(execution)).thenReturn("gpt-4");
        when(modelRouter.generateWithFallback(anyString(), anyString(), anyDouble()))
                .thenReturn("Direct answer without any tool calls");

        thinkingStateHandler.handle(stateMachine, execution);

        verify(stateMachine, times(1)).transition(AgentExecutionState.COMPLETED, execution);
    }

    @Test
    void handleShouldTransitionToToolCallingWhenResponseContainsToolCalls() {
        when(chatMemoryStore.loadMessages("conv-123")).thenReturn(List.of());
        when(modelResolver.resolve(execution)).thenReturn("gpt-4");
        when(modelRouter.generateWithFallback(anyString(), anyString(), anyDouble()))
                .thenReturn("I will call a tool: <tool>search</tool>");
        when(loopDetection.detectLoop(anyLong(), anyString(), anyList()))
                .thenReturn(LoopDetectionResult.noLoop());

        thinkingStateHandler.handle(stateMachine, execution);

        verify(stateMachine, times(1)).transition(AgentExecutionState.TOOL_CALLING, execution);
    }

    @Test
    void handleShouldTransitionToFailedOnException() {
        when(chatMemoryStore.loadMessages("conv-123"))
                .thenThrow(new RuntimeException("Memory store unavailable"));

        thinkingStateHandler.handle(stateMachine, execution);

        verify(stateMachine, times(1)).transition(AgentExecutionState.FAILED, execution);
    }

    @Test
    void handleShouldConsumeTokenBudget() {
        when(chatMemoryStore.loadMessages("conv-123")).thenReturn(List.of());
        when(modelResolver.resolve(execution)).thenReturn("gpt-4");
        when(modelRouter.generateWithFallback(anyString(), anyString(), anyDouble()))
                .thenReturn("Short response");

        thinkingStateHandler.handle(stateMachine, execution);

        assertNotNull(execution.getTokenBudgetJson());
        assertTrue(execution.getTokenBudgetJson().contains(","));
    }

    @Test
    void handleShouldTransitionToGateBlockedWhenBudgetExceeded() {
        execution.setTokenBudgetJson("10,10,0,0");
        when(chatMemoryStore.loadMessages("conv-123")).thenReturn(List.of(
                new LlmMessage("user", "This is a very long message that will exceed the token budget")
        ));

        thinkingStateHandler.handle(stateMachine, execution);

        verify(stateMachine, times(1)).transition(AgentExecutionState.GATE_BLOCKED, execution);
    }

    @Test
    void handleShouldSkipBudgetCheckWhenNoBudgetSet() {
        execution.setTokenBudgetJson(null);
        when(chatMemoryStore.loadMessages("conv-123")).thenReturn(List.of());
        when(modelResolver.resolve(execution)).thenReturn("gpt-4");
        when(modelRouter.generateWithFallback(anyString(), anyString(), anyDouble()))
                .thenReturn("Answer without budget");

        thinkingStateHandler.handle(stateMachine, execution);

        verify(stateMachine, times(1)).transition(AgentExecutionState.COMPLETED, execution);
    }

    @Test
    void handleShouldDetectToolCallsWithFunctionTag() {
        when(chatMemoryStore.loadMessages("conv-123")).thenReturn(List.of());
        when(modelResolver.resolve(execution)).thenReturn("gpt-4");
        when(modelRouter.generateWithFallback(anyString(), anyString(), anyDouble()))
                .thenReturn("Calling <function>getWeather</function>");
        when(loopDetection.detectLoop(anyLong(), anyString(), anyList()))
                .thenReturn(LoopDetectionResult.noLoop());

        thinkingStateHandler.handle(stateMachine, execution);

        verify(stateMachine, times(1)).transition(AgentExecutionState.TOOL_CALLING, execution);
    }

    @Test
    void handleShouldDetectToolCallsWithCodeBlock() {
        when(chatMemoryStore.loadMessages("conv-123")).thenReturn(List.of());
        when(modelResolver.resolve(execution)).thenReturn("gpt-4");
        when(modelRouter.generateWithFallback(anyString(), anyString(), anyDouble()))
                .thenReturn("```tool\nsearch\n```");
        when(loopDetection.detectLoop(anyLong(), anyString(), anyList()))
                .thenReturn(LoopDetectionResult.noLoop());

        thinkingStateHandler.handle(stateMachine, execution);

        verify(stateMachine, times(1)).transition(AgentExecutionState.TOOL_CALLING, execution);
    }

    @Test
    void handleShouldUseDefaultModelWhenExecutionHasNoModelConfig() {
        when(chatMemoryStore.loadMessages("conv-123")).thenReturn(List.of());
        when(modelResolver.resolve(execution)).thenReturn("gpt-4");
        when(modelRouter.generateWithFallback(anyString(), anyString(), anyDouble()))
                .thenReturn("Response");

        thinkingStateHandler.handle(stateMachine, execution);

        verify(modelResolver, times(1)).resolve(execution);
        verify(modelRouter, times(1)).generateWithFallback(anyString(), eq("gpt-4"), anyDouble());
    }

    @Test
    void handleShouldUseModelFromMetadataWhenAvailable() {
        execution.setMetadata("modelId", "claude-3-sonnet");
        when(chatMemoryStore.loadMessages("conv-123")).thenReturn(List.of());
        when(modelResolver.resolve(execution)).thenReturn("claude-3-sonnet");
        when(modelRouter.generateWithFallback(anyString(), eq("claude-3-sonnet"), anyDouble()))
                .thenReturn("Response");

        thinkingStateHandler.handle(stateMachine, execution);

        verify(modelResolver, times(1)).resolve(execution);
        verify(modelRouter, times(1)).generateWithFallback(anyString(), eq("claude-3-sonnet"), anyDouble());
    }
}
