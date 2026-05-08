package com.schemaplexai.agent.engine.a2a;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Agent metadata record describing a remote agent's capabilities and endpoint.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentCard {

    private String name;
    private String version;
    private List<String> capabilities;
    private String endpointUrl;
    private String authenticationType;
    private int maxConcurrentExecutions;
    private List<String> supportedMessageFormats;
}
