package com.schemaplexai.agent.engine.model;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class OpenAiProvider implements LlmProvider {

    @Override
    public String generate(String prompt, String modelId, Double temperature) {
        log.info("OpenAI generating with model {}", modelId);
        // Integration with langchain4j OpenAI client
        return "";
    }

    @Override
    public String generateWithMessages(List<LlmMessage> messages, String modelId, Double temperature) {
        log.info("OpenAI chat completion with model {}", modelId);
        return "";
    }

    @Override
    public boolean isHealthy() {
        return true;
    }

    @Override
    public String getProviderName() {
        return "OPENAI";
    }
}
