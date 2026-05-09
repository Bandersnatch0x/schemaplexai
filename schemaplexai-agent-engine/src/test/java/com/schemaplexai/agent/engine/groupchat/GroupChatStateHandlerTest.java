package com.schemaplexai.agent.engine.groupchat;

import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.model.LlmProvider;
import com.schemaplexai.agent.engine.state.AgentExecutionState;
import com.schemaplexai.agent.engine.state.AgentStateMachine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GroupChatStateHandlerTest {

    @Mock
    private GroupChatOrchestrator orchestrator;

    @Mock
    private LlmProvider llmProvider;

    @Mock
    private AgentStateMachine stateMachine;

    private GroupChatStateHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GroupChatStateHandler(orchestrator, llmProvider);
    }

    @Test
    void returnsCorrectState() {
        assertThat(handler.getState()).isEqualTo(AgentExecutionState.GROUP_CHAT);
    }

    @Test
    void handle_delegatesToOrchestratorWithDefaultModel() {
        SfAgentExecution execution = createExecution();
        // no modelId metadata

        handler.handle(stateMachine, execution);

        verify(orchestrator).runGroupChat(stateMachine, execution, llmProvider, "gpt-4o");
    }

    @Test
    void handle_usesMetadataModelId() {
        SfAgentExecution execution = createExecution();
        execution.setMetadata("modelId", "gpt-4o-mini");

        handler.handle(stateMachine, execution);

        verify(orchestrator).runGroupChat(stateMachine, execution, llmProvider, "gpt-4o-mini");
    }

    private SfAgentExecution createExecution() {
        SfAgentExecution execution = new SfAgentExecution();
        execution.setId(200L);
        execution.setAgentId(1L);
        execution.setTenantId("tenant-1");
        execution.setConversationId("conv-200");
        execution.setState(AgentExecutionState.GROUP_CHAT.name());
        return execution;
    }
}
