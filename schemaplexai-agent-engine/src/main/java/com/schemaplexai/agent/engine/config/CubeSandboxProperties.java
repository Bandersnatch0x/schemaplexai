package com.schemaplexai.agent.engine.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "cube.sandbox")
public class CubeSandboxProperties {

    private String apiUrl = "http://localhost:8080";

    private String apiKey = "";

    private int requestTimeoutSeconds = 30;

    private int maxSandboxes = 10;
}
