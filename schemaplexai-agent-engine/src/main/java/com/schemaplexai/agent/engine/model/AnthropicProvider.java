package com.schemaplexai.agent.engine.model;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class AnthropicProvider implements LlmProvider {

    @Override
    public String generate(String prompt, String modelId, Double temperature) {
        log.info("Anthropic generating with model {}", modelId);
        return "";
    }

    @Override
    public String generateWithMessages(List<LlmMessage> messages, String modelId, Double temperature) {
        log.info("Anthropic chat completion with model {}", modelId);
        return "";
    }

    @Override
    public boolean isHealthy() {
        return true;
    }

    @Override
    public String getProviderName() {
        return "ANTHROPIC";
    }
}
