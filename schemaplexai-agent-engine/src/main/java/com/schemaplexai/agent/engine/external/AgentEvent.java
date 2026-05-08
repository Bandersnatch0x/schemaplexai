package com.schemaplexai.agent.engine.external;

import java.time.Instant;

/**
 * Event emitted by an external agent adapter.
 */
public class AgentEvent {

    private String type;
    private String payload;
    private Instant timestamp;

    public AgentEvent(String type, String payload) {
        this.type = type;
        this.payload = payload;
        this.timestamp = Instant.now();
    }

    public String getType() {
        return type;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
