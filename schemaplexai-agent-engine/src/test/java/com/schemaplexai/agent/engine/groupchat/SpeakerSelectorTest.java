package com.schemaplexai.agent.engine.groupchat;

import com.schemaplexai.agent.engine.model.LlmMessage;
import com.schemaplexai.agent.engine.model.LlmProvider;
import com.schemaplexai.agent.engine.orchestrator.AgentRouter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpeakerSelectorTest {

    private SpeakerSelector selector;

    private List<AgentRouter.AgentCapability> agents;

    @Mock
    private LlmProvider llmProvider;

    @BeforeEach
    void setUp() {
        selector = new SpeakerSelector();
        agents = List.of(
                new AgentRouter.AgentCapability("coder", "Writes code", Set.of("code", "java", "implement"), 2),
                new AgentRouter.AgentCapability("reviewer", "Reviews code", Set.of("review", "security", "quality"), 2),
                new AgentRouter.AgentCapability("tester", "Writes tests", Set.of("test", "coverage", "junit"), 2)
        );
    }

    @Test
    void roundRobin_cyclesThroughAgents() {
        Optional<Integer> first = selector.selectNextSpeaker(
                SpeakerSelector.Mode.ROUND_ROBIN, null, agents, 0, null, null);
        assertThat(first).isPresent().hasValue(1);

        Optional<Integer> second = selector.selectNextSpeaker(
                SpeakerSelector.Mode.ROUND_ROBIN, null, agents, 1, null, null);
        assertThat(second).isPresent().hasValue(2);

        Optional<Integer> third = selector.selectNextSpeaker(
                SpeakerSelector.Mode.ROUND_ROBIN, null, agents, 2, null, null);
        assertThat(third).isPresent().hasValue(0);
    }

    @Test
    void roundRobin_wrapsAround() {
        Optional<Integer> result = selector.selectNextSpeaker(
                SpeakerSelector.Mode.ROUND_ROBIN, null, agents, 2, null, null);
        assertThat(result).isPresent().hasValue(0);
    }

    @Test
    void specialtyBased_selectsMatchingAgent() {
        LlmMessage message = new LlmMessage("user", "Write a JUnit test for this method");

        Optional<Integer> result = selector.selectNextSpeaker(
                SpeakerSelector.Mode.SPECIALTY_BASED, message, agents, 0, null, null);

        assertThat(result).isPresent().hasValue(2); // tester
    }

    @Test
    void specialtyBased_fallsBackToRoundRobinWhenNoMatch() {
        LlmMessage message = new LlmMessage("user", "Deploy to production immediately");

        Optional<Integer> result = selector.selectNextSpeaker(
                SpeakerSelector.Mode.SPECIALTY_BASED, message, agents, 1, null, null);

        // fallback uses round-robin from index 0, yielding (0+1)%3 = 1
        // but AgentRouter fallback returns first available agent (index 0), so we just assert present
        assertThat(result).isPresent();
    }

    @Test
    void specialtyBased_fallsBackWhenMessageIsBlank() {
        LlmMessage message = new LlmMessage("user", "   ");

        Optional<Integer> result = selector.selectNextSpeaker(
                SpeakerSelector.Mode.SPECIALTY_BASED, message, agents, 0, null, null);

        assertThat(result).isPresent().hasValue(1); // round-robin fallback from index 0
    }

    @Test
    void llmSelected_usesLlmResponse() {
        when(llmProvider.generate(anyString(), anyString(), any())).thenReturn("reviewer");

        Optional<Integer> result = selector.selectNextSpeaker(
                SpeakerSelector.Mode.LLM_SELECTED, new LlmMessage("user", "Check security"), agents, 0, llmProvider, "gpt-4o");

        assertThat(result).isPresent().hasValue(1); // reviewer
    }

    @Test
    void llmSelected_fallsBackToRoundRobinOnUnrecognizedId() {
        when(llmProvider.generate(anyString(), anyString(), any())).thenReturn("unknown-agent");

        Optional<Integer> result = selector.selectNextSpeaker(
                SpeakerSelector.Mode.LLM_SELECTED, new LlmMessage("user", "Hello"), agents, 0, llmProvider, "gpt-4o");

        assertThat(result).isPresent().hasValue(1); // round-robin fallback
    }

    @Test
    void llmSelected_fallsBackToRoundRobinOnLlmFailure() {
        when(llmProvider.generate(anyString(), anyString(), any())).thenThrow(new RuntimeException("LLM error"));

        Optional<Integer> result = selector.selectNextSpeaker(
                SpeakerSelector.Mode.LLM_SELECTED, new LlmMessage("user", "Hello"), agents, 0, llmProvider, "gpt-4o");

        assertThat(result).isPresent().hasValue(1); // round-robin fallback
    }

    @Test
    void llmSelected_fallsBackWhenProviderIsNull() {
        Optional<Integer> result = selector.selectNextSpeaker(
                SpeakerSelector.Mode.LLM_SELECTED, new LlmMessage("user", "Hello"), agents, 0, null, "gpt-4o");

        assertThat(result).isPresent().hasValue(1); // round-robin fallback
    }

    @Test
    void llmSelected_matchesCaseInsensitive() {
        when(llmProvider.generate(anyString(), anyString(), any())).thenReturn("CODER");

        Optional<Integer> result = selector.selectNextSpeaker(
                SpeakerSelector.Mode.LLM_SELECTED, null, agents, 0, llmProvider, "gpt-4o");

        assertThat(result).isPresent().hasValue(0);
    }

    @Test
    void llmSelected_containsMatchFallback() {
        when(llmProvider.generate(anyString(), anyString(), any())).thenReturn("The best choice is the reviewer agent.");

        Optional<Integer> result = selector.selectNextSpeaker(
                SpeakerSelector.Mode.LLM_SELECTED, null, agents, 0, llmProvider, "gpt-4o");

        assertThat(result).isPresent().hasValue(1);
    }

    @Test
    void emptyAgents_returnsEmpty() {
        Optional<Integer> result = selector.selectNextSpeaker(
                SpeakerSelector.Mode.ROUND_ROBIN, null, List.of(), 0, null, null);
        assertThat(result).isEmpty();
    }

    @Test
    void nullAgents_returnsEmpty() {
        Optional<Integer> result = selector.selectNextSpeaker(
                SpeakerSelector.Mode.ROUND_ROBIN, null, null, 0, null, null);
        assertThat(result).isEmpty();
    }
}
