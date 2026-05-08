package com.schemaplexai.agent.engine.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for the agent execution engine.
 */
@Data
@Component
@ConfigurationProperties(prefix = "agent.engine")
public class AgentEngineProperties {

    private int maxToolCalls = 10;
}
