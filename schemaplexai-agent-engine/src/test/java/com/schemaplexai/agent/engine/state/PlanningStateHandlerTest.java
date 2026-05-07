package com.schemaplexai.agent.engine.state;

import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.memory.CompositeChatMemoryStore;
import com.schemaplexai.agent.engine.model.AiModelRouter;
import com.schemaplexai.agent.engine.model.LlmMessage;
import com.schemaplexai.agent.engine.model.ModelResolver;
import com.schemaplexai.agent.engine.plan.SubTask;
import com.schemaplexai.agent.engine.plan.SubTaskPlan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanningStateHandlerTest {

    @Mock
    private AiModelRouter modelRouter;

    @Mock
    private CompositeChatMemoryStore chatMemoryStore;

    @Mock
    private ModelResolver modelResolver;

    @Mock
    private AgentStateMachine stateMachine;

    @InjectMocks
    private PlanningStateHandler handler;

    private SfAgentExecution execution;

    @BeforeEach
    void setUp() {
        execution = new SfAgentExecution();
        execution.setId(1L);
        execution.setAgentId(42L);
        execution.setConversationId("conv-123");
    }

    @Test
    void getStateShouldReturnPlanning() {
        assertEquals(AgentExecutionState.PLANNING, handler.getState());
    }

    @Test
    void handleShouldLoadMessagesFromMemory() {
        when(chatMemoryStore.loadMessages("conv-123")).thenReturn(List.of(
                new LlmMessage("user", "Build a REST API")
        ));
        when(modelResolver.resolve(execution)).thenReturn("gpt-4");
        when(modelRouter.generateWithFallback(anyString(), eq("gpt-4"), eq(0.4)))
                .thenReturn("1. Design the data model\n2. Implement controllers\n3. Write tests");

        handler.handle(stateMachine, execution);

        verify(chatMemoryStore, times(1)).loadMessages("conv-123");
    }

    @Test
    void handleShouldCallLlmWithPlanningPrompt() {
        when(chatMemoryStore.loadMessages("conv-123")).thenReturn(List.of(
                new LlmMessage("user", "Build a REST API")
        ));
        when(modelResolver.resolve(execution)).thenReturn("gpt-4");
        when(modelRouter.generateWithFallback(anyString(), eq("gpt-4"), eq(0.4)))
                .thenReturn("1. Design the data model\n2. Implement controllers");

        handler.handle(stateMachine, execution);

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(modelRouter).generateWithFallback(promptCaptor.capture(), eq("gpt-4"), eq(0.4));
        String prompt = promptCaptor.getValue();
        assertTrue(prompt.contains("Build a REST API"));
        assertTrue(prompt.contains("task decomposition"));
    }

    @Test
    void handleShouldStorePlanInMetadataAndTransitionToThinking() {
        when(chatMemoryStore.loadMessages("conv-123")).thenReturn(List.of(
                new LlmMessage("user", "Build a REST API")
        ));
        when(modelResolver.resolve(execution)).thenReturn("gpt-4");
        when(modelRouter.generateWithFallback(anyString(), eq("gpt-4"), eq(0.4)))
                .thenReturn("1. Design the data model\n2. Implement controllers\n3. Write tests");

        handler.handle(stateMachine, execution);

        verify(stateMachine).saveExecution(execution);
        verify(stateMachine).transition(AgentExecutionState.THINKING, execution);

        Object planMeta = execution.getMetadata("subTaskPlan");
        assertNotNull(planMeta);
        assertTrue(planMeta.toString().contains("Design the data model"));
    }

    @Test
    void handleShouldTransitionToThinkingWhenLlmReturnsEmpty() {
        when(chatMemoryStore.loadMessages("conv-123")).thenReturn(List.of(
                new LlmMessage("user", "Say hello")
        ));
        when(modelResolver.resolve(execution)).thenReturn("gpt-4");
        when(modelRouter.generateWithFallback(anyString(), eq("gpt-4"), eq(0.4)))
                .thenReturn("");

        handler.handle(stateMachine, execution);

        verify(stateMachine).saveExecution(execution);
        verify(stateMachine).transition(AgentExecutionState.THINKING, execution);
    }

    @Test
    void handleShouldTransitionToThinkingOnException() {
        when(chatMemoryStore.loadMessages("conv-123"))
                .thenThrow(new RuntimeException("Memory store unavailable"));

        handler.handle(stateMachine, execution);

        verify(stateMachine, times(1)).transition(AgentExecutionState.THINKING, execution);
        verify(stateMachine, never()).saveExecution(any());
    }

    @Test
    void buildPlanningPromptShouldContainGoal() {
        String prompt = handler.buildPlanningPrompt("Build a microservice");

        assertTrue(prompt.contains("Build a microservice"));
        assertTrue(prompt.contains("task decomposition"));
        assertTrue(prompt.contains("Sub-tasks:"));
    }

    @Test
    void buildPlanningPromptShouldHandleNullGoal() {
        String prompt = handler.buildPlanningPrompt("null goal test");

        assertTrue(prompt.contains("null goal test"));
    }

    @Test
    void parseSubTaskPlanShouldExtractNumberedItems() {
        String llmOutput = "1. Design the data model\n2. Implement controllers\n3. Write tests";

        SubTaskPlan plan = handler.parseSubTaskPlan("Build API", llmOutput);

        assertEquals("Build API", plan.getGoal());
        assertEquals(3, plan.getSubTasks().size());
        assertEquals("Design the data model", plan.getSubTasks().get(0).getDescription());
        assertEquals("Implement controllers", plan.getSubTasks().get(1).getDescription());
        assertEquals("Write tests", plan.getSubTasks().get(2).getDescription());
    }

    @Test
    void parseSubTaskPlanShouldExtractBulletItems() {
        String llmOutput = "- Design the data model\n- Implement controllers\n- Write tests";

        SubTaskPlan plan = handler.parseSubTaskPlan("Build API", llmOutput);

        assertEquals(3, plan.getSubTasks().size());
        assertEquals("Design the data model", plan.getSubTasks().get(0).getDescription());
    }

    @Test
    void parseSubTaskPlanShouldAssignSequentialIds() {
        String llmOutput = "1. Task A\n2. Task B";

        SubTaskPlan plan = handler.parseSubTaskPlan("Goal", llmOutput);

        assertEquals(2, plan.getSubTasks().size());
        assertTrue(plan.getSubTasks().get(0).getId().startsWith("st-1-"));
        assertTrue(plan.getSubTasks().get(1).getId().startsWith("st-2-"));
    }

    @Test
    void parseSubTaskPlanShouldSetDefaultDependencies() {
        String llmOutput = "1. Task A\n2. Task B\n3. Task C";

        SubTaskPlan plan = handler.parseSubTaskPlan("Goal", llmOutput);

        assertTrue(plan.getSubTasks().get(0).getDependencies().isEmpty());
        assertEquals(1, plan.getSubTasks().get(1).getDependencies().size());
        assertEquals(plan.getSubTasks().get(0).getId(), plan.getSubTasks().get(1).getDependencies().get(0));
        assertEquals(plan.getSubTasks().get(1).getId(), plan.getSubTasks().get(2).getDependencies().get(0));
    }

    @Test
    void parseSubTaskPlanShouldSetPendingStatus() {
        String llmOutput = "1. Task A";

        SubTaskPlan plan = handler.parseSubTaskPlan("Goal", llmOutput);

        assertEquals(SubTask.STATUS_PENDING, plan.getSubTasks().get(0).getStatus());
    }

    @Test
    void parseSubTaskPlanShouldHandleEmptyOutput() {
        SubTaskPlan plan = handler.parseSubTaskPlan("Goal", "");

        assertTrue(plan.getSubTasks().isEmpty());
    }

    @Test
    void parseSubTaskPlanShouldHandleNullOutput() {
        SubTaskPlan plan = handler.parseSubTaskPlan("Goal", null);

        assertTrue(plan.getSubTasks().isEmpty());
    }

    @Test
    void parseSubTaskPlanShouldSkipShortLines() {
        String llmOutput = "1. Task A\n2. OK\n3. Task B";

        SubTaskPlan plan = handler.parseSubTaskPlan("Goal", llmOutput);

        // "OK" has length 2, which is <= 3, so it should be skipped
        assertEquals(2, plan.getSubTasks().size());
    }

    @Test
    void serializeAndDeserializePlanShouldRoundTrip() throws Exception {
        SubTaskPlan original = handler.parseSubTaskPlan("Goal", "1. Task A\n2. Task B");

        String json = handler.serializePlan(original);
        SubTaskPlan restored = handler.deserializePlan(json);

        assertNotNull(restored);
        assertEquals("Goal", restored.getGoal());
        assertEquals(2, restored.getSubTasks().size());
        assertEquals("Task A", restored.getSubTasks().get(0).getDescription());
    }

    @Test
    void deserializePlanShouldReturnNullForNullJson() {
        assertNull(handler.deserializePlan(null));
    }

    @Test
    void deserializePlanShouldReturnNullForBlankJson() {
        assertNull(handler.deserializePlan("   "));
    }

    @Test
    void deserializePlanShouldReturnNullForInvalidJson() {
        assertNull(handler.deserializePlan("not valid json"));
    }

    @Test
    void handleShouldUseResolvedModel() {
        when(chatMemoryStore.loadMessages("conv-123")).thenReturn(List.of(
                new LlmMessage("user", "Hello")
        ));
        when(modelResolver.resolve(execution)).thenReturn("claude-3-sonnet");
        when(modelRouter.generateWithFallback(anyString(), eq("claude-3-sonnet"), eq(0.4)))
                .thenReturn("1. Say hello back");

        handler.handle(stateMachine, execution);

        verify(modelResolver).resolve(execution);
        verify(modelRouter).generateWithFallback(anyString(), eq("claude-3-sonnet"), eq(0.4));
    }

    @Test
    void handleShouldWorkWithNoUserMessage() {
        when(chatMemoryStore.loadMessages("conv-123")).thenReturn(List.of(
                new LlmMessage("assistant", "Hello")
        ));
        when(modelResolver.resolve(execution)).thenReturn("gpt-4");
        when(modelRouter.generateWithFallback(anyString(), eq("gpt-4"), eq(0.4)))
                .thenReturn("1. Acknowledge");

        handler.handle(stateMachine, execution);

        verify(stateMachine).transition(AgentExecutionState.THINKING, execution);
    }
}
