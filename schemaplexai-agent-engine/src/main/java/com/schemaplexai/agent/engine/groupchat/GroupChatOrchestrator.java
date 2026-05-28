package com.schemaplexai.agent.engine.groupchat;

import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.model.LlmMessage;
import com.schemaplexai.agent.engine.model.LlmProvider;
import com.schemaplexai.agent.engine.orchestrator.AgentRouter;
import com.schemaplexai.agent.engine.sse.ExecutionEventBus;
import com.schemaplexai.agent.engine.state.AgentExecutionState;
import com.schemaplexai.agent.engine.state.AgentStateMachine;
import com.schemaplexai.common.context.TenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

/**
 * Orchestrates a multi-agent group chat conversation.
 *
 * <p>A group chat brings multiple specialist agents together to collaboratively
 * solve a task through a moderated conversation loop. The orchestrator manages:
 * <ul>
 *   <li>Speaker selection via {@link SpeakerSelector}</li>
 *   <li>Consensus detection via {@link ConsensusDetector}</li>
 *   <li>Round limits and timeouts</li>
 *   <li>SSE event broadcasting for real-time UI updates</li>
 * </ul>
 *
 * <p>Configuration (via execution metadata):
 * <ul>
 *   <li>{@code groupChatAgents} — list of {@link AgentRouter.AgentCapability} participants</li>
 *   <li>{@code groupChatMode} — {@link SpeakerSelector.Mode} (default ROUND_ROBIN)</li>
 *   <li>{@code groupChatMaxRounds} — max conversation rounds (default 3)</li>
 *   <li>{@code groupChatTimeoutSeconds} — timeout in seconds (default 300)</li>
 *   <li>{@code groupChatConsensusThreshold} — keyword threshold for consensus (default 2)</li>
 *   <li>{@code groupChatPrompt} — the shared task/prompt for the group</li>
 * </ul>
 */
@Slf4j
@Component
public class GroupChatOrchestrator {

    private static final int DEFAULT_MAX_ROUNDS = 3;
    private static final int DEFAULT_TIMEOUT_SECONDS = 300;
    private static final int DEFAULT_CONSENSUS_THRESHOLD = 2;

    private final SpeakerSelector speakerSelector;
    private final ConsensusDetector consensusDetector;
    private final ExecutionEventBus eventBus;

    // In-memory conversation state keyed by execution ID
    private final Map<Long, GroupChatSession> sessions = new ConcurrentHashMap<>();

    public GroupChatOrchestrator(SpeakerSelector speakerSelector,
                                 ConsensusDetector consensusDetector,
                                 ExecutionEventBus eventBus) {
        this.speakerSelector = speakerSelector;
        this.consensusDetector = consensusDetector;
        this.eventBus = eventBus;
    }

    /**
     * Runs a group chat session for the given execution.
     *
     * @param stateMachine state machine for the parent execution
     * @param execution    parent execution containing group-chat metadata
     * @param llmProvider  LLM provider for agent generation and LLM-based speaker selection
     * @param modelId      model ID for LLM calls
     */
    @SuppressWarnings("unchecked")
    public void runGroupChat(AgentStateMachine stateMachine,
                             SfAgentExecution execution,
                             LlmProvider llmProvider,
                             String modelId) {

        List<AgentRouter.AgentCapability> agents =
                (List<AgentRouter.AgentCapability>) execution.getMetadata("groupChatAgents");
        if (agents == null || agents.size() < 2) {
            log.error("Group chat requires at least 2 agents, execution {}", execution.getId());
            stateMachine.transition(AgentExecutionState.FAILED, execution);
            return;
        }

        SpeakerSelector.Mode mode = resolveMode((String) execution.getMetadata("groupChatMode"));
        int maxRounds = resolveInt(execution.getMetadata("groupChatMaxRounds"), DEFAULT_MAX_ROUNDS);
        int timeoutSeconds = resolveInt(execution.getMetadata("groupChatTimeoutSeconds"), DEFAULT_TIMEOUT_SECONDS);
        int consensusThreshold = resolveInt(execution.getMetadata("groupChatConsensusThreshold"), DEFAULT_CONSENSUS_THRESHOLD);
        String taskPrompt = (String) execution.getMetadata("groupChatPrompt");
        if (taskPrompt == null || taskPrompt.isBlank()) {
            taskPrompt = "Collaborate to solve the assigned task.";
        }

        String tenantId = execution.getTenantId();
        Long tenantIdLong = parseTenantId(tenantId);

        GroupChatSession session = new GroupChatSession(
                execution.getId(),
                execution.getConversationId(),
                agents,
                mode,
                maxRounds,
                Duration.ofSeconds(timeoutSeconds),
                consensusThreshold,
                taskPrompt
        );
        sessions.put(execution.getId(), session);

        eventBus.publishOutput(execution.getId(),
                "Group chat started with " + agents.size() + " agents, mode=" + mode
                        + ", maxRounds=" + maxRounds, tenantIdLong);

        try {
            executeConversationLoop(stateMachine, execution, session, llmProvider, modelId, tenantId, tenantIdLong);
        } catch (TimeoutException e) {
            log.warn("Group chat timed out for execution {}", execution.getId());
            eventBus.publishError(execution.getId(), "Group chat timed out after " + timeoutSeconds + "s", tenantIdLong);
            execution.setMetadata("groupChatResult", "TIMEOUT");
            stateMachine.transition(AgentExecutionState.FAILED, execution);
        } catch (Exception e) {
            log.error("Group chat failed for execution {}", execution.getId(), e);
            eventBus.publishError(execution.getId(), "Group chat error: " + e.getMessage(), tenantIdLong);
            execution.setMetadata("groupChatResult", "ERROR: " + e.getMessage());
            stateMachine.transition(AgentExecutionState.FAILED, execution);
        } finally {
            sessions.remove(execution.getId());
        }
    }

    private void executeConversationLoop(AgentStateMachine stateMachine,
                                         SfAgentExecution execution,
                                         GroupChatSession session,
                                         LlmProvider llmProvider,
                                         String modelId,
                                         String tenantId,
                                         Long tenantIdLong) throws TimeoutException {
        Instant deadline = Instant.now().plus(session.timeout);
        int currentSpeakerIndex = 0;

        // Seed the conversation with the task prompt
        session.messages.add(new LlmMessage("system", "Task: " + session.taskPrompt));
        eventBus.publishPlan(execution.getId(), "Task: " + session.taskPrompt, tenantIdLong);

        for (int round = 0; round < session.maxRounds; round++) {
            if (Instant.now().isAfter(deadline)) {
                throw new TimeoutException("Deadline exceeded");
            }

            eventBus.publishOutput(execution.getId(), "--- Round " + (round + 1) + " ---", tenantIdLong);

            for (int turn = 0; turn < session.agents.size(); turn++) {
                if (Instant.now().isAfter(deadline)) {
                    throw new TimeoutException("Deadline exceeded during turn");
                }

                LlmMessage lastMessage = session.messages.isEmpty()
                        ? new LlmMessage("system", session.taskPrompt)
                        : session.messages.get(session.messages.size() - 1);

                Optional<Integer> selected = speakerSelector.selectNextSpeaker(
                        session.mode, lastMessage, session.agents, currentSpeakerIndex, llmProvider, modelId);

                if (selected.isEmpty()) {
                    log.warn("No speaker selected in group chat, execution {}", execution.getId());
                    break;
                }

                currentSpeakerIndex = selected.get();
                AgentRouter.AgentCapability speaker = session.agents.get(currentSpeakerIndex);

                // Build context for the agent
                String context = buildConversationContext(session.messages);
                String agentPrompt = "You are " + speaker.agentId() + ": " + speaker.description()
                        + "\n\nConversation so far:\n" + context
                        + "\n\nProvide your contribution.";

                eventBus.publishThought(execution.getId(),
                        "Agent " + speaker.agentId() + " speaking...", tenantIdLong);

                String response;
                try {
                    TenantContextHolder.setTenantId(tenantId);
                    response = llmProvider.generate(agentPrompt, modelId, 0.7);
                } finally {
                    TenantContextHolder.clear();
                }

                LlmMessage agentMessage = new LlmMessage("assistant", response);
                session.messages.add(agentMessage);
                session.agentResponses.add(response);

                eventBus.publishOutput(execution.getId(),
                        speaker.agentId() + ": " + response, tenantIdLong);

                // Check consensus after each agent speaks
                ConsensusDetector.ConsensusResult consensus = consensusDetector.detectConsensus(
                        session.agentResponses, session.agents.size(), session.consensusThreshold);

                if (consensus.consensusReached()) {
                    log.info("Consensus reached in group chat execution {}: {}",
                            execution.getId(), consensus.reason());
                    eventBus.publishOutput(execution.getId(),
                            "Consensus reached: " + consensus.reason(), tenantIdLong);
                    finishGroupChat(stateMachine, execution, session, consensus.reason(), tenantIdLong);
                    return;
                }
            }
        }

        // Max rounds reached without consensus
        log.info("Group chat completed without consensus, execution {}", execution.getId());
        finishGroupChat(stateMachine, execution, session, "Max rounds reached without consensus", tenantIdLong);
    }

    private void finishGroupChat(AgentStateMachine stateMachine,
                                 SfAgentExecution execution,
                                 GroupChatSession session,
                                 String finishReason,
                                 Long tenantIdLong) {
        String summary = buildConversationContext(session.messages);
        execution.setMetadata("groupChatResult", finishReason);
        execution.setMetadata("groupChatSummary", summary);
        execution.setMetadata("groupChatMessages", session.messages);
        eventBus.publishOutput(execution.getId(),
                "Group chat finished: " + finishReason, tenantIdLong);
        stateMachine.transition(AgentExecutionState.COMPLETED, execution);
    }

    private String buildConversationContext(List<LlmMessage> messages) {
        StringBuilder sb = new StringBuilder();
        for (LlmMessage msg : messages) {
            sb.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n\n");
        }
        return sb.toString();
    }

    private SpeakerSelector.Mode resolveMode(String value) {
        if (value == null || value.isBlank()) {
            return SpeakerSelector.Mode.ROUND_ROBIN;
        }
        try {
            return SpeakerSelector.Mode.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown group chat mode '{}', defaulting to ROUND_ROBIN", value);
            return SpeakerSelector.Mode.ROUND_ROBIN;
        }
    }

    private int resolveInt(Object value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private Long parseTenantId(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(tenantId);
        } catch (NumberFormatException e) {
            String[] parts = tenantId.split("-");
            if (parts.length > 0) {
                try {
                    return Long.valueOf(parts[parts.length - 1]);
                } catch (NumberFormatException ex) {
                    return null;
                }
            }
            return null;
        }
    }

    /**
     * Returns the active session for an execution, or null if none.
     */
    public GroupChatSession getSession(Long executionId) {
        return sessions.get(executionId);
    }

    /**
     * Immutable(ish) session state for an in-flight group chat.
     */
    public static class GroupChatSession {
        private final Long executionId;
        private final String conversationId;
        private final List<AgentRouter.AgentCapability> agents;
        private final SpeakerSelector.Mode mode;
        private final int maxRounds;
        private final Duration timeout;
        private final int consensusThreshold;
        private final String taskPrompt;
        private final List<LlmMessage> messages = new ArrayList<>();
        private final List<String> agentResponses = new ArrayList<>();

        public GroupChatSession(Long executionId,
                                String conversationId,
                                List<AgentRouter.AgentCapability> agents,
                                SpeakerSelector.Mode mode,
                                int maxRounds,
                                Duration timeout,
                                int consensusThreshold,
                                String taskPrompt) {
            this.executionId = executionId;
            this.conversationId = conversationId != null ? conversationId : UUID.randomUUID().toString();
            this.agents = List.copyOf(agents);
            this.mode = mode;
            this.maxRounds = maxRounds;
            this.timeout = timeout;
            this.consensusThreshold = consensusThreshold;
            this.taskPrompt = taskPrompt;
        }

        public Long getExecutionId() { return executionId; }
        public String getConversationId() { return conversationId; }
        public List<AgentRouter.AgentCapability> getAgents() { return agents; }
        public SpeakerSelector.Mode getMode() { return mode; }
        public int getMaxRounds() { return maxRounds; }
        public Duration getTimeout() { return timeout; }
        public int getConsensusThreshold() { return consensusThreshold; }
        public String getTaskPrompt() { return taskPrompt; }
        public List<LlmMessage> getMessages() { return List.copyOf(messages); }
        public List<String> getAgentResponses() { return List.copyOf(agentResponses); }
    }
}
