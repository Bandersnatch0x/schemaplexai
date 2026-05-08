package com.schemaplexai.agent.engine.orchestrator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AgentRouterTest {

    private AgentRouter router;

    private AgentRouter.AgentCapability coderAgent;
    private AgentRouter.AgentCapability reviewerAgent;
    private AgentRouter.AgentCapability testerAgent;

    @BeforeEach
    void setUp() {
        router = new AgentRouter();

        coderAgent = new AgentRouter.AgentCapability(
                "coder-1", "Writes code", Set.of("code", "function", "implement", "write", "java"), 3
        );
        reviewerAgent = new AgentRouter.AgentCapability(
                "reviewer-1", "Reviews code", Set.of("review", "check", "quality", "security"), 2
        );
        testerAgent = new AgentRouter.AgentCapability(
                "tester-1", "Writes tests", Set.of("test", "unit", "integration", "coverage"), 2
        );
    }

    @Test
    void shouldRouteToAgentWithMatchingKeywords() {
        Optional<AgentRouter.AgentCapability> result = router.route(
                "Write a Java function to calculate factorial",
                List.of(coderAgent, reviewerAgent, testerAgent)
        );

        assertThat(result).isPresent();
        assertThat(result.get().agentId()).isEqualTo("coder-1");
    }

    @Test
    void shouldRouteToReviewerForReviewTasks() {
        Optional<AgentRouter.AgentCapability> result = router.route(
                "Review this code for security issues",
                List.of(coderAgent, reviewerAgent, testerAgent)
        );

        assertThat(result).isPresent();
        assertThat(result.get().agentId()).isEqualTo("reviewer-1");
    }

    @Test
    void shouldRouteToTesterForTestTasks() {
        Optional<AgentRouter.AgentCapability> result = router.route(
                "Write unit tests for the calculator class",
                List.of(coderAgent, reviewerAgent, testerAgent)
        );

        assertThat(result).isPresent();
        assertThat(result.get().agentId()).isEqualTo("tester-1");
    }

    @Test
    void shouldReturnEmptyForBlankTaskDescription() {
        Optional<AgentRouter.AgentCapability> result = router.route(
                "",
                List.of(coderAgent)
        );

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyForNullTaskDescription() {
        Optional<AgentRouter.AgentCapability> result = router.route(
                null,
                List.of(coderAgent)
        );

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyForNullAgentList() {
        Optional<AgentRouter.AgentCapability> result = router.route(
                "Write code", null
        );

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyForEmptyAgentList() {
        Optional<AgentRouter.AgentCapability> result = router.route(
                "Write code", List.of()
        );

        assertThat(result).isEmpty();
    }

    @Test
    void shouldFallbackToFirstAgentWhenNoKeywordsMatch() {
        AgentRouter.AgentCapability nicheAgent = new AgentRouter.AgentCapability(
                "niche-1", "Very niche", Set.of("blockchain", "crypto"), 1
        );

        Optional<AgentRouter.AgentCapability> result = router.route(
                "Deploy to production",
                List.of(nicheAgent, coderAgent)
        );

        // No keywords match, fallback to first available
        assertThat(result).isPresent();
        assertThat(result.get().agentId()).isEqualTo("niche-1");
    }

    @Test
    void shouldReturnEmptyWhenNoKeywordsMatchAndListIsEmpty() {
        // This is covered by the empty agent list test, but let's also test
        // with agents that have empty keyword sets
        AgentRouter.AgentCapability noKeywordsAgent = new AgentRouter.AgentCapability(
                "empty-1", "No keywords", Set.of(), 1
        );

        Optional<AgentRouter.AgentCapability> result = router.route(
                "Do something",
                List.of(noKeywordsAgent)
        );

        // Fallback: returns first agent even if score is 0
        assertThat(result).isPresent();
        assertThat(result.get().agentId()).isEqualTo("empty-1");
    }

    @Test
    void computeScoreShouldReturnZeroForEmptyKeywords() {
        int score = router.computeScore(Set.of("code", "java"), Set.of());
        assertThat(score).isZero();
    }

    @Test
    void computeScoreShouldCountMatchingKeywords() {
        int score = router.computeScore(Set.of("code", "java", "function"), Set.of("code", "python"));
        assertThat(score).isEqualTo(1);
    }

    @Test
    void computeScoreShouldHandleSubstringMatching() {
        // "coder" contains "code"
        int score = router.computeScore(Set.of("coder"), Set.of("code"));
        assertThat(score).isEqualTo(1);
    }

    @Test
    void computeScoreShouldHandleMultipleMatches() {
        int score = router.computeScore(
                Set.of("write", "java", "code"),
                Set.of("code", "java", "function")
        );
        assertThat(score).isEqualTo(2);
    }

    @Test
    void shouldHandleSingleAgent() {
        Optional<AgentRouter.AgentCapability> result = router.route(
                "Write code",
                List.of(coderAgent)
        );

        assertThat(result).isPresent();
        assertThat(result.get().agentId()).isEqualTo("coder-1");
    }

    @Test
    void agentCapabilityShouldRejectBlankAgentId() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                new AgentRouter.AgentCapability("", "desc", Set.of("key"), 1)
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("agentId");
    }

    @Test
    void agentCapabilityShouldDefaultKeywordsToEmpty() {
        AgentRouter.AgentCapability agent = new AgentRouter.AgentCapability(
                "agent-1", "desc", null, 1
        );
        assertThat(agent.keywords()).isEmpty();
    }

    @Test
    void agentCapabilityShouldEnforceMinimumConcurrent() {
        AgentRouter.AgentCapability agent = new AgentRouter.AgentCapability(
                "agent-1", "desc", Set.of("key"), 0
        );
        assertThat(agent.maxConcurrent()).isEqualTo(1);
    }
}
