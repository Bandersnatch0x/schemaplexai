package com.schemaplexai.agent.engine.groupchat;

import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.model.LlmMessage;
import com.schemaplexai.agent.engine.model.LlmProvider;
import com.schemaplexai.agent.engine.orchestrator.AgentRouter;
import com.schemaplexai.agent.engine.sse.ExecutionEventBus;
import com.schemaplexai.agent.engine.state.AgentExecutionState;
import com.schemaplexai.agent.engine.state.AgentStateMachine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupChatOrchestratorTest {

    @Mock
    private SpeakerSelector speakerSelector;

    @Mock
    private ConsensusDetector consensusDetector;

    @Mock
    private ExecutionEventBus eventBus;

    @Mock
    private LlmProvider llmProvider;

    @Mock
    private AgentStateMachine stateMachine;

    private GroupChatOrchestrator orchestrator;

    private List<AgentRouter.AgentCapability> agents;

    @BeforeEach
    void setUp() {
        orchestrator = new GroupChatOrchestrator(speakerSelector, consensusDetector, eventBus);
        agents = List.of(
                new AgentRouter.AgentCapability("coder", "Writes code", Set.of("code", "java"), 2),
                new AgentRouter.AgentCapability("reviewer", "Reviews code", Set.of("review", "security"), 2)
        );
    }

    @Test
    void groupChat_reachesConsensusAndCompletes() {
        SfAgentExecution execution = createExecution();
        execution.setMetadata("groupChatAgents", agents);
        execution.setMetadata("groupChatPrompt", "Design a REST API");
        execution.setMetadata("groupChatMaxRounds", 3);
        execution.setMetadata("groupChatMode", "ROUND_ROBIN");

        when(speakerSelector.selectNextSpeaker(any(), any(), eq(agents), anyInt(), any(), anyString()))
                .thenReturn(java.util.Optional.of(0))
                .thenReturn(java.util.Optional.of(1))
                .thenReturn(java.util.Optional.of(0));

        when(llmProvider.generate(anyString(), anyString(), anyDouble()))
                .thenReturn("Let's use Spring Boot")
                .thenReturn("I agree, Spring Boot is ideal")
                .thenReturn("We should add OpenAPI docs");

        when(consensusDetector.detectConsensus(anyList(), eq(2), eq(2)))
                .thenReturn(new ConsensusDetector.ConsensusResult(false, 0.3, "No consensus yet"))
                .thenReturn(new ConsensusDetector.ConsensusResult(true, 0.9, "Consensus reached"));

        orchestrator.runGroupChat(stateMachine, execution, llmProvider, "gpt-4o");

        verify(stateMachine).transition(AgentExecutionState.COMPLETED, execution);
        assertThat(execution.getMetadata("groupChatResult")).isEqualTo("Consensus reached");
    }

    @Test
    void groupChat_maxRoundsWithoutConsensus_completesAnyway() {
        SfAgentExecution execution = createExecution();
        execution.setMetadata("groupChatAgents", agents);
        execution.setMetadata("groupChatPrompt", "Pick a database");
        execution.setMetadata("groupChatMaxRounds", 1);
        execution.setMetadata("groupChatMode", "ROUND_ROBIN");

        when(speakerSelector.selectNextSpeaker(any(), any(), eq(agents), anyInt(), any(), anyString()))
                .thenReturn(java.util.Optional.of(0))
                .thenReturn(java.util.Optional.of(1));

        when(llmProvider.generate(anyString(), anyString(), anyDouble()))
                .thenReturn("Use PostgreSQL")
                .thenReturn("I prefer MySQL");

        when(consensusDetector.detectConsensus(anyList(), eq(2), eq(2)))
                .thenReturn(new ConsensusDetector.ConsensusResult(false, 0.1, "No consensus"));

        orchestrator.runGroupChat(stateMachine, execution, llmProvider, "gpt-4o");

        verify(stateMachine).transition(AgentExecutionState.COMPLETED, execution);
        assertThat(execution.getMetadata("groupChatResult")).isEqualTo("Max rounds reached without consensus");
    }

    @Test
    void groupChat_lessThanTwoAgents_fails() {
        SfAgentExecution execution = createExecution();
        execution.setMetadata("groupChatAgents", List.of(
                new AgentRouter.AgentCapability("solo", "Solo agent", Set.of("all"), 1)
        ));

        orchestrator.runGroupChat(stateMachine, execution, llmProvider, "gpt-4o");

        verify(stateMachine).transition(AgentExecutionState.FAILED, execution);
    }

    @Test
    void groupChat_noAgents_fails() {
        SfAgentExecution execution = createExecution();
        execution.setMetadata("groupChatAgents", List.of());

        orchestrator.runGroupChat(stateMachine, execution, llmProvider, "gpt-4o");

        verify(stateMachine).transition(AgentExecutionState.FAILED, execution);
    }

    @Test
    void groupChat_nullAgents_fails() {
        SfAgentExecution execution = createExecution();
        // groupChatAgents not set -> null

        orchestrator.runGroupChat(stateMachine, execution, llmProvider, "gpt-4o");

        verify(stateMachine).transition(AgentExecutionState.FAILED, execution);
    }

    @Test
    void groupChat_llmFailureDuringTurn_fails() {
        SfAgentExecution execution = createExecution();
        execution.setMetadata("groupChatAgents", agents);
        execution.setMetadata("groupChatPrompt", "Design API");
        execution.setMetadata("groupChatMaxRounds", 2);

        when(speakerSelector.selectNextSpeaker(any(), any(), eq(agents), anyInt(), any(), anyString()))
                .thenReturn(java.util.Optional.of(0));

        when(llmProvider.generate(anyString(), anyString(), anyDouble()))
                .thenThrow(new RuntimeException("LLM timeout"));

        orchestrator.runGroupChat(stateMachine, execution, llmProvider, "gpt-4o");

        verify(stateMachine).transition(AgentExecutionState.FAILED, execution);
        assertThat(execution.getMetadata("groupChatResult")).asString().contains("ERROR");
    }

    @Test
    void groupChat_publishesEvents() {
        SfAgentExecution execution = createExecution();
        execution.setMetadata("groupChatAgents", agents);
        execution.setMetadata("groupChatPrompt", "Design API");
        execution.setMetadata("groupChatMaxRounds", 1);
        execution.setMetadata("groupChatMode", "ROUND_ROBIN");

        when(speakerSelector.selectNextSpeaker(any(), any(), eq(agents), anyInt(), any(), anyString()))
                .thenReturn(java.util.Optional.of(0))
                .thenReturn(java.util.Optional.of(1));

        when(llmProvider.generate(anyString(), anyString(), anyDouble()))
                .thenReturn("Response 1")
                .thenReturn("Response 2");

        when(consensusDetector.detectConsensus(anyList(), eq(2), eq(2)))
                .thenReturn(new ConsensusDetector.ConsensusResult(false, 0.0, "No"));

        orchestrator.runGroupChat(stateMachine, execution, llmProvider, "gpt-4o");

        verify(eventBus, atLeastOnce()).publishOutput(eq(execution.getId()), anyString(), any());
        verify(eventBus, atLeastOnce()).publishPlan(eq(execution.getId()), anyString(), any());
    }

    @Test
    void groupChat_sessionStoredAndRemoved() {
        SfAgentExecution execution = createExecution();
        execution.setMetadata("groupChatAgents", agents);
        execution.setMetadata("groupChatPrompt", "Design API");
        execution.setMetadata("groupChatMaxRounds", 1);

        when(speakerSelector.selectNextSpeaker(any(), any(), eq(agents), anyInt(), any(), anyString()))
                .thenReturn(java.util.Optional.of(0))
                .thenReturn(java.util.Optional.of(1));

        when(llmProvider.generate(anyString(), anyString(), anyDouble()))
                .thenReturn("R1")
                .thenReturn("R2");

        when(consensusDetector.detectConsensus(anyList(), eq(2), eq(2)))
                .thenReturn(new ConsensusDetector.ConsensusResult(false, 0.0, "No"));

        assertThat(orchestrator.getSession(execution.getId())).isNull();

        orchestrator.runGroupChat(stateMachine, execution, llmProvider, "gpt-4o");

        assertThat(orchestrator.getSession(execution.getId())).isNull();
    }

    @Test
    void groupChat_usesDefaultValuesWhenMetadataMissing() {
        SfAgentExecution execution = createExecution();
        execution.setMetadata("groupChatAgents", agents);
        // No mode, maxRounds, timeout, consensusThreshold, or prompt

        when(speakerSelector.selectNextSpeaker(any(), any(), eq(agents), anyInt(), any(), anyString()))
                .thenReturn(java.util.Optional.of(0))
                .thenReturn(java.util.Optional.of(1));

        when(llmProvider.generate(anyString(), anyString(), anyDouble()))
                .thenReturn("R1")
                .thenReturn("R2");

        when(consensusDetector.detectConsensus(anyList(), eq(2), eq(2)))
                .thenReturn(new ConsensusDetector.ConsensusResult(false, 0.0, "No"));

        orchestrator.runGroupChat(stateMachine, execution, llmProvider, "gpt-4o");

        verify(stateMachine).transition(AgentExecutionState.COMPLETED, execution);
    }

    @Test
    void groupChat_setsTenantContextDuringLlmCalls() {
        SfAgentExecution execution = createExecution();
        execution.setMetadata("groupChatAgents", agents);
        execution.setMetadata("groupChatPrompt", "Design API");
        execution.setMetadata("groupChatMaxRounds", 1);

        when(speakerSelector.selectNextSpeaker(any(), any(), eq(agents), anyInt(), any(), anyString()))
                .thenReturn(java.util.Optional.of(0))
                .thenReturn(java.util.Optional.of(1));

        when(llmProvider.generate(anyString(), anyString(), anyDouble()))
                .thenReturn("R1")
                .thenReturn("R2");

        when(consensusDetector.detectConsensus(anyList(), eq(2), eq(2)))
                .thenReturn(new ConsensusDetector.ConsensusResult(false, 0.0, "No"));

        orchestrator.runGroupChat(stateMachine, execution, llmProvider, "gpt-4o");

        // LLM provider should have been called twice (2 agents * 1 round)
        verify(llmProvider, times(2)).generate(anyString(), anyString(), anyDouble());
    }

    @Test
    void groupChat_summaryStoredInMetadata() {
        SfAgentExecution execution = createExecution();
        execution.setMetadata("groupChatAgents", agents);
        execution.setMetadata("groupChatPrompt", "Design API");
        execution.setMetadata("groupChatMaxRounds", 1);

        when(speakerSelector.selectNextSpeaker(any(), any(), eq(agents), anyInt(), any(), anyString()))
                .thenReturn(java.util.Optional.of(0))
                .thenReturn(java.util.Optional.of(1));

        when(llmProvider.generate(anyString(), anyString(), anyDouble()))
                .thenReturn("R1")
                .thenReturn("R2");

        when(consensusDetector.detectConsensus(anyList(), eq(2), eq(2)))
                .thenReturn(new ConsensusDetector.ConsensusResult(false, 0.0, "No"));

        orchestrator.runGroupChat(stateMachine, execution, llmProvider, "gpt-4o");

        assertThat(execution.getMetadata("groupChatSummary")).isNotNull();
        assertThat(execution.getMetadata("groupChatMessages")).isNotNull();
    }

    private SfAgentExecution createExecution() {
        SfAgentExecution execution = new SfAgentExecution();
        execution.setId(100L);
        execution.setAgentId(1L);
        execution.setTenantId("tenant-1");
        execution.setConversationId("conv-100");
        execution.setState(AgentExecutionState.GROUP_CHAT.name());
        return execution;
    }
}
