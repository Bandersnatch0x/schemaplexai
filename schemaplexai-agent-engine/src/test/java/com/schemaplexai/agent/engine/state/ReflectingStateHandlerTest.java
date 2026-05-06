package com.schemaplexai.agent.engine.state;

import com.schemaplexai.agent.engine.evaluation.ReflectionResult;
import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.guardrails.GuardrailsEngine;
import com.schemaplexai.agent.engine.memory.CompositeChatMemoryStore;
import com.schemaplexai.agent.engine.model.AiModelRouter;
import com.schemaplexai.agent.engine.model.LlmMessage;
import com.schemaplexai.agent.engine.model.ModelResolver;
import org.junit.jupiter.api.BeforeEach;
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
class ReflectingStateHandlerTest {

    @Mock
    private AiModelRouter modelRouter;

    @Mock
    private GuardrailsEngine guardrailsEngine;

    @Mock
    private CompositeChatMemoryStore chatMemoryStore;

    @Mock
    private ModelResolver modelResolver;

    @Mock
    private AgentStateMachine stateMachine;

    @InjectMocks
    private ReflectingStateHandler handler;

    private SfAgentExecution execution;

    @BeforeEach
    void setUp() {
        execution = new SfAgentExecution();
        execution.setId(1L);
        execution.setAgentId(42L);
        execution.setConversationId("conv-123");
    }

    @Test
    void getStateShouldReturnReflecting() {
        assertEquals(AgentExecutionState.REFLECTING, handler.getState());
    }

    @Test
    void handleShouldTransitionToCompletedWhenMaxReflectionsReached() {
        execution.setTokenBudgetJson("reflections=2");

        handler.handle(stateMachine, execution);

        verify(stateMachine).transition(AgentExecutionState.COMPLETED, execution);
        verifyNoInteractions(chatMemoryStore, modelRouter, guardrailsEngine);
    }

    @Test
    void handleShouldTransitionToFailedWhenGuardrailsBlock() {
        execution.setTokenBudgetJson("reflections=0");
        when(chatMemoryStore.loadMessages("conv-123")).thenReturn(List.of(
                new LlmMessage("user", "Hello"),
                new LlmMessage("assistant", "Bad output")
        ));
        when(guardrailsEngine.validateOutput("Bad output"))
                .thenReturn(new com.schemaplexai.agent.engine.evaluation.ValidationResult(false, "Blocked"));

        handler.handle(stateMachine, execution);

        verify(stateMachine).transition(AgentExecutionState.FAILED, execution);
        verify(modelRouter, never()).generateWithFallback(any(), any(), anyDouble());
    }

    @Test
    void handleShouldTransitionToCompletedWhenReflectionPasses() {
        execution.setTokenBudgetJson("reflections=0");
        when(chatMemoryStore.loadMessages("conv-123")).thenReturn(List.of(
                new LlmMessage("user", "Hello"),
                new LlmMessage("assistant", "Good output")
        ));
        when(guardrailsEngine.validateOutput("Good output"))
                .thenReturn(com.schemaplexai.agent.engine.evaluation.ValidationResult.valid());
        when(modelResolver.resolve(execution)).thenReturn("gpt-4");
        when(modelRouter.generateWithFallback(anyString(), eq("gpt-4"), eq(0.3)))
                .thenReturn("PASS - the response looks good");

        handler.handle(stateMachine, execution);

        verify(stateMachine).transition(AgentExecutionState.COMPLETED, execution);
    }

    @Test
    void handleShouldTransitionToThinkingWhenRevisionNeeded() {
        execution.setTokenBudgetJson("reflections=0");
        when(chatMemoryStore.loadMessages("conv-123")).thenReturn(List.of(
                new LlmMessage("user", "Hello"),
                new LlmMessage("assistant", "Mediocre output")
        ));
        when(guardrailsEngine.validateOutput("Mediocre output"))
                .thenReturn(com.schemaplexai.agent.engine.evaluation.ValidationResult.valid());
        when(modelResolver.resolve(execution)).thenReturn("gpt-4");
        when(modelRouter.generateWithFallback(anyString(), eq("gpt-4"), eq(0.3)))
                .thenReturn("REVISE: Add more details about the topic");

        handler.handle(stateMachine, execution);

        verify(stateMachine).saveExecution(execution);
        verify(stateMachine).transition(AgentExecutionState.THINKING, execution);
        assertEquals("reflections=1", execution.getTokenBudgetJson());
    }

    @Test
    void handleShouldTransitionToCompletedOnException() {
        execution.setTokenBudgetJson("reflections=0");
        when(chatMemoryStore.loadMessages("conv-123")).thenReturn(List.of(
                new LlmMessage("assistant", "Output")
        ));
        when(guardrailsEngine.validateOutput("Output"))
                .thenReturn(com.schemaplexai.agent.engine.evaluation.ValidationResult.valid());
        when(modelResolver.resolve(execution)).thenReturn("gpt-4");
        when(modelRouter.generateWithFallback(anyString(), eq("gpt-4"), eq(0.3)))
                .thenThrow(new RuntimeException("LLM error"));

        handler.handle(stateMachine, execution);

        verify(stateMachine).transition(AgentExecutionState.COMPLETED, execution);
    }

    @Test
    void handleShouldSkipGuardrailsWhenNoAssistantMessage() {
        execution.setTokenBudgetJson("reflections=0");
        when(chatMemoryStore.loadMessages("conv-123")).thenReturn(List.of(
                new LlmMessage("user", "Hello")
        ));
        when(modelResolver.resolve(execution)).thenReturn("gpt-4");
        when(modelRouter.generateWithFallback(anyString(), eq("gpt-4"), eq(0.3)))
                .thenReturn("PASS");

        handler.handle(stateMachine, execution);

        verify(guardrailsEngine, never()).validateOutput(any());
        verify(stateMachine).transition(AgentExecutionState.COMPLETED, execution);
    }

    @Test
    void handleShouldHandleEmptyMessages() {
        execution.setTokenBudgetJson("reflections=0");
        when(chatMemoryStore.loadMessages("conv-123")).thenReturn(List.of());
        when(modelResolver.resolve(execution)).thenReturn("gpt-4");
        when(modelRouter.generateWithFallback(anyString(), eq("gpt-4"), eq(0.3)))
                .thenReturn("PASS");

        handler.handle(stateMachine, execution);

        verify(stateMachine).transition(AgentExecutionState.COMPLETED, execution);
    }

    @Test
    void buildReflectionPromptShouldFormatCorrectly() {
        String prompt = handler.buildReflectionPrompt("What is AI?", "AI stands for artificial intelligence.");

        assertTrue(prompt.contains("What is AI?"));
        assertTrue(prompt.contains("AI stands for artificial intelligence."));
        assertTrue(prompt.contains("PASS"));
        assertTrue(prompt.contains("REVISE"));
    }

    @Test
    void buildReflectionPromptShouldHandleNullInputs() {
        String prompt = handler.buildReflectionPrompt(null, null);

        assertTrue(prompt.contains("(unknown)"));
        assertTrue(prompt.contains("(no output)"));
    }

    @Test
    void parseReflectionResultShouldReturnAcceptedForNull() {
        ReflectionResult result = handler.parseReflectionResult(null);
        assertTrue(result.accepted());
    }

    @Test
    void parseReflectionResultShouldReturnAcceptedForBlank() {
        ReflectionResult result = handler.parseReflectionResult("   ");
        assertTrue(result.accepted());
    }

    @Test
    void parseReflectionResultShouldReturnAcceptedForPass() {
        ReflectionResult result = handler.parseReflectionResult("The response is PASS. Good job!");
        assertTrue(result.accepted());
    }

    @Test
    void parseReflectionResultShouldReturnNeedsRevisionForRevise() {
        ReflectionResult result = handler.parseReflectionResult("REVISE: Add more examples");
        assertTrue(result.needsRevision());
        assertEquals("Add more examples", result.suggestions());
    }

    @Test
    void parseReflectionResultShouldBeCaseInsensitive() {
        ReflectionResult result = handler.parseReflectionResult("pass");
        assertTrue(result.accepted());

        ReflectionResult result2 = handler.parseReflectionResult("revise: fix grammar");
        assertTrue(result2.needsRevision());
    }

    @Test
    void parseReflectionResultShouldDefaultToAcceptedForUnknownFormat() {
        ReflectionResult result = handler.parseReflectionResult("Some random text without pass or revise");
        assertTrue(result.accepted());
    }

    @Test
    void getReflectionCountShouldParseFromBudget() {
        execution.setTokenBudgetJson("reflections=3,maxInput=1000");
        assertEquals(3, handler.getReflectionCount(execution));
    }

    @Test
    void getReflectionCountShouldReturnZeroWhenNotPresent() {
        execution.setTokenBudgetJson("maxInput=1000");
        assertEquals(0, handler.getReflectionCount(execution));
    }

    @Test
    void getReflectionCountShouldReturnZeroForNullBudget() {
        execution.setTokenBudgetJson(null);
        assertEquals(0, handler.getReflectionCount(execution));
    }

    @Test
    void getReflectionCountShouldHandleInvalidNumber() {
        execution.setTokenBudgetJson("reflections=abc");
        assertEquals(0, handler.getReflectionCount(execution));
    }

    @Test
    void handleShouldUpdateReflectionCountInExistingBudget() {
        execution.setTokenBudgetJson("reflections=1,other=data");
        when(chatMemoryStore.loadMessages("conv-123")).thenReturn(List.of(
                new LlmMessage("assistant", "Output")
        ));
        when(guardrailsEngine.validateOutput("Output"))
                .thenReturn(com.schemaplexai.agent.engine.evaluation.ValidationResult.valid());
        when(modelResolver.resolve(execution)).thenReturn("gpt-4");
        when(modelRouter.generateWithFallback(anyString(), eq("gpt-4"), eq(0.3)))
                .thenReturn("REVISE: improve");

        handler.handle(stateMachine, execution);

        assertEquals("reflections=2,other=data", execution.getTokenBudgetJson());
    }

    @Test
    void handleShouldAppendReflectionCountToBudgetWithoutIt() {
        execution.setTokenBudgetJson("maxInput=1000");
        when(chatMemoryStore.loadMessages("conv-123")).thenReturn(List.of(
                new LlmMessage("assistant", "Output")
        ));
        when(guardrailsEngine.validateOutput("Output"))
                .thenReturn(com.schemaplexai.agent.engine.evaluation.ValidationResult.valid());
        when(modelResolver.resolve(execution)).thenReturn("gpt-4");
        when(modelRouter.generateWithFallback(anyString(), eq("gpt-4"), eq(0.3)))
                .thenReturn("REVISE: improve");

        handler.handle(stateMachine, execution);

        assertEquals("maxInput=1000,reflections=1", execution.getTokenBudgetJson());
    }

    @Test
    void handleShouldSetReflectionCountWhenBudgetIsEmpty() {
        execution.setTokenBudgetJson("");
        when(chatMemoryStore.loadMessages("conv-123")).thenReturn(List.of(
                new LlmMessage("assistant", "Output")
        ));
        when(guardrailsEngine.validateOutput("Output"))
                .thenReturn(com.schemaplexai.agent.engine.evaluation.ValidationResult.valid());
        when(modelResolver.resolve(execution)).thenReturn("gpt-4");
        when(modelRouter.generateWithFallback(anyString(), eq("gpt-4"), eq(0.3)))
                .thenReturn("REVISE: improve");

        handler.handle(stateMachine, execution);

        assertEquals("reflections=1", execution.getTokenBudgetJson());
    }
}
