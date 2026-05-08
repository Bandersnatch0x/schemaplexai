package com.schemaplexai.agent.engine.a2a;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Message envelope for Agent-to-Agent (A2A) protocol communication.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class A2aMessage {

    private String messageId;
    private String senderAgentId;
    private String recipientAgentId;
    private MessageType messageType;
    private String payload;
    private Instant timestamp;
    private String correlationId;

    /**
     * A2A message types.
     */
    public enum MessageType {
        REQUEST,
        RESPONSE,
        STREAM,
        ERROR
    }
}
