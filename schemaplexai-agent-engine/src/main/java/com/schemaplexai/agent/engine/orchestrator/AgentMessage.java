package com.schemaplexai.agent.engine.orchestrator;

import java.time.Instant;
import java.util.Map;

/**
 * Immutable message for inter-agent communication.
 *
 * @param fromAgentId  sender agent identifier
 * @param toAgentId    target agent identifier
 * @param messageType  message type (e.g. "subtask", "result", "error")
 * @param content      message body
 * @param metadata     additional key-value metadata
 * @param timestamp    creation time
 */
public record AgentMessage(
        String fromAgentId,
        String toAgentId,
        String messageType,
        String content,
        Map<String, Object> metadata,
        Instant timestamp
) {

    public AgentMessage {
        if (fromAgentId == null || fromAgentId.isBlank()) {
            throw new IllegalArgumentException("fromAgentId must not be blank");
        }
        if (toAgentId == null || toAgentId.isBlank()) {
            throw new IllegalArgumentException("toAgentId must not be blank");
        }
        if (messageType == null || messageType.isBlank()) {
            throw new IllegalArgumentException("messageType must not be blank");
        }
        if (timestamp == null) {
            timestamp = Instant.now();
        }
        if (metadata == null) {
            metadata = Map.of();
        }
    }

    /**
     * Convenience factory for a sub-task message.
     */
    public static AgentMessage subtask(String fromAgentId, String toAgentId, String content) {
        return new AgentMessage(fromAgentId, toAgentId, "subtask", content, Map.of(), Instant.now());
    }

    /**
     * Convenience factory for a result message.
     */
    public static AgentMessage result(String fromAgentId, String toAgentId, String content) {
        return new AgentMessage(fromAgentId, toAgentId, "result", content, Map.of(), Instant.now());
    }

    /**
     * Convenience factory for an error message.
     */
    public static AgentMessage error(String fromAgentId, String toAgentId, String content) {
        return new AgentMessage(fromAgentId, toAgentId, "error", content, Map.of(), Instant.now());
    }
}
