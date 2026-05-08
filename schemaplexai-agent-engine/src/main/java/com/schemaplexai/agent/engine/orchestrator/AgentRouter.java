package com.schemaplexai.agent.engine.orchestrator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Routes tasks to the best-matching agent based on keyword-based capability matching.
 * <p>
 * V1 implementation uses simple keyword overlap scoring. A future version may
 * delegate to an LLM for semantic matching.
 */
@Slf4j
@Component
public class AgentRouter {

    /**
     * Describes an agent's capabilities for routing purposes.
     *
     * @param agentId       unique agent identifier
     * @param description   human-readable description of what the agent does
     * @param keywords      set of keywords the agent is specialized in
     * @param maxConcurrent maximum concurrent tasks this agent can handle
     */
    public record AgentCapability(
            String agentId,
            String description,
            Set<String> keywords,
            int maxConcurrent
    ) {
        public AgentCapability {
            if (agentId == null || agentId.isBlank()) {
                throw new IllegalArgumentException("agentId must not be blank");
            }
            if (keywords == null) {
                keywords = Set.of();
            }
            if (maxConcurrent < 1) {
                maxConcurrent = 1;
            }
        }
    }

    /**
     * Routes a task description to the best-matching agent.
     *
     * @param taskDescription the task to route
     * @param availableAgents list of available agent capabilities
     * @return the best matching agent, or empty if no match exceeds the threshold
     */
    public Optional<AgentCapability> route(String taskDescription, List<AgentCapability> availableAgents) {
        if (taskDescription == null || taskDescription.isBlank()) {
            log.warn("Cannot route: task description is blank");
            return Optional.empty();
        }
        if (availableAgents == null || availableAgents.isEmpty()) {
            log.warn("Cannot route: no available agents");
            return Optional.empty();
        }

        Set<String> taskTokens = tokenize(taskDescription);

        return availableAgents.stream()
                .map(agent -> new ScoredAgent(agent, computeScore(taskTokens, agent.keywords())))
                .filter(scored -> scored.score > 0)
                .max(Comparator.comparingInt(ScoredAgent::score))
                .map(ScoredAgent::agent)
                .or(() -> {
                    log.info("No agent matched task '{}', falling back to first available agent", taskDescription);
                    return availableAgents.stream().findFirst();
                });
    }

    /**
     * Computes a keyword overlap score between task tokens and agent keywords.
     */
    int computeScore(Set<String> taskTokens, Set<String> agentKeywords) {
        if (agentKeywords.isEmpty()) {
            return 0;
        }
        int score = 0;
        for (String keyword : agentKeywords) {
            String normalizedKeyword = keyword.toLowerCase(Locale.ROOT);
            for (String token : taskTokens) {
                if (token.contains(normalizedKeyword) || normalizedKeyword.contains(token)) {
                    score++;
                }
            }
        }
        return score;
    }

    /**
     * Tokenizes a task description into normalized lowercase words.
     */
    static Set<String> tokenize(String text) {
        return Set.of(text.toLowerCase(Locale.ROOT).split("\\W+"));
    }

    private record ScoredAgent(AgentCapability agent, int score) {}
}
