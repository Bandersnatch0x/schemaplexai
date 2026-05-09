package com.schemaplexai.agent.engine.groupchat;

import com.schemaplexai.agent.engine.model.LlmMessage;
import com.schemaplexai.agent.engine.model.LlmProvider;
import com.schemaplexai.agent.engine.orchestrator.AgentRouter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Selects the next speaker in a group chat conversation.
 *
 * <p>Supports three modes:
 * <ul>
 *   <li>{@link Mode#ROUND_ROBIN} — cycles through agents in order</li>
 *   <li>{@link Mode#LLM_SELECTED} — asks an LLM to pick the best next speaker</li>
 *   <li>{@link Mode#SPECIALTY_BASED} — routes to the agent whose keywords best match the last message</li>
 * </ul>
 */
@Slf4j
@Component
public class SpeakerSelector {

    private static final String LLM_SELECTOR_PROMPT = """
            You are a conversation facilitator. Based on the conversation history,
            select the most appropriate next speaker from the available agents.

            Available agents:
            %s

            Respond with ONLY the agent ID of the next speaker. No explanation.
            """;

    /**
     * Speaker selection strategy.
     */
    public enum Mode {
        ROUND_ROBIN,
        LLM_SELECTED,
        SPECIALTY_BASED
    }

    /**
     * Selects the next speaker index given the current conversation state.
     *
     * @param mode            selection strategy
     * @param lastMessage     the most recent message in the conversation
     * @param agents          list of participating agent capabilities
     * @param currentIndex    index of the current speaker (for round-robin continuity)
     * @param llmProvider     LLM provider (required for LLM_SELECTED mode)
     * @param modelId         model ID for LLM selection
     * @return index of the selected next speaker, or empty if no valid selection
     */
    public Optional<Integer> selectNextSpeaker(
            Mode mode,
            LlmMessage lastMessage,
            List<AgentRouter.AgentCapability> agents,
            int currentIndex,
            LlmProvider llmProvider,
            String modelId) {

        if (agents == null || agents.isEmpty()) {
            log.warn("SpeakerSelector: no agents available");
            return Optional.empty();
        }

        return switch (mode) {
            case ROUND_ROBIN -> selectRoundRobin(agents, currentIndex);
            case LLM_SELECTED -> selectViaLlm(lastMessage, agents, llmProvider, modelId);
            case SPECIALTY_BASED -> selectBySpecialty(lastMessage, agents);
        };
    }

    private Optional<Integer> selectRoundRobin(List<AgentRouter.AgentCapability> agents, int currentIndex) {
        int nextIndex = (currentIndex + 1) % agents.size();
        log.debug("Round-robin selection: {} -> {}", currentIndex, nextIndex);
        return Optional.of(nextIndex);
    }

    private Optional<Integer> selectViaLlm(
            LlmMessage lastMessage,
            List<AgentRouter.AgentCapability> agents,
            LlmProvider llmProvider,
            String modelId) {

        if (llmProvider == null) {
            log.warn("SpeakerSelector LLM_SELECTED mode requires an LlmProvider; falling back to ROUND_ROBIN");
            return selectRoundRobin(agents, 0);
        }

        String agentList = buildAgentList(agents);
        String prompt = String.format(LLM_SELECTOR_PROMPT, agentList);

        if (lastMessage != null && lastMessage.getContent() != null && !lastMessage.getContent().isBlank()) {
            prompt = prompt + "\n\nLast message: " + lastMessage.getContent();
        }

        try {
            String response = llmProvider.generate(prompt, modelId, 0.2).trim();
            log.debug("LLM speaker selection response: {}", response);

            // Try exact match first
            for (int i = 0; i < agents.size(); i++) {
                if (agents.get(i).agentId().equalsIgnoreCase(response)) {
                    return Optional.of(i);
                }
            }

            // Fallback: case-insensitive contains match
            String lowerResponse = response.toLowerCase(Locale.ROOT);
            for (int i = 0; i < agents.size(); i++) {
                if (lowerResponse.contains(agents.get(i).agentId().toLowerCase(Locale.ROOT))) {
                    return Optional.of(i);
                }
            }

            log.warn("LLM returned unrecognized agent ID '{}', falling back to round-robin", response);
            return selectRoundRobin(agents, 0);
        } catch (Exception e) {
            log.error("LLM speaker selection failed, falling back to round-robin", e);
            return selectRoundRobin(agents, 0);
        }
    }

    private Optional<Integer> selectBySpecialty(LlmMessage lastMessage,
                                                 List<AgentRouter.AgentCapability> agents) {
        if (lastMessage == null || lastMessage.getContent() == null || lastMessage.getContent().isBlank()) {
            log.debug("No last message for specialty routing, falling back to round-robin");
            return selectRoundRobin(agents, 0);
        }

        AgentRouter router = new AgentRouter();
        var matched = router.route(lastMessage.getContent(), agents);
        if (matched.isPresent()) {
            String matchedId = matched.get().agentId();
            for (int i = 0; i < agents.size(); i++) {
                if (agents.get(i).agentId().equals(matchedId)) {
                    log.debug("Specialty-based selection: agent '{}' at index {}", matchedId, i);
                    return Optional.of(i);
                }
            }
        }

        log.debug("No specialty match found, falling back to round-robin");
        return selectRoundRobin(agents, 0);
    }

    private String buildAgentList(List<AgentRouter.AgentCapability> agents) {
        StringBuilder sb = new StringBuilder();
        for (AgentRouter.AgentCapability agent : agents) {
            sb.append("- ").append(agent.agentId()).append(": ").append(agent.description());
            if (agent.keywords() != null && !agent.keywords().isEmpty()) {
                sb.append(" (keywords: ").append(String.join(", ", agent.keywords())).append(")");
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
