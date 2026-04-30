package com.schemaplexai.agent.engine.model;

import java.util.List;

public interface LlmProvider {

    String generate(String prompt, String modelId, Double temperature);

    String generateWithMessages(List<LlmMessage> messages, String modelId, Double temperature);

    boolean isHealthy();

    String getProviderName();
}
