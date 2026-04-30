package com.schemaplexai.agent.engine.context;

import com.schemaplexai.agent.engine.model.LlmMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@Component
public class ContextInjector {

    private static final List<Pattern> DANGEROUS_PATTERNS = List.of(
        Pattern.compile("ignore\\s+previous\\s+instructions", Pattern.CASE_INSENSITIVE),
        Pattern.compile("ignore\\s+the\\s+above", Pattern.CASE_INSENSITIVE),
        Pattern.compile("disregard", Pattern.CASE_INSENSITIVE),
        Pattern.compile("system\\s+prompt", Pattern.CASE_INSENSITIVE),
        Pattern.compile("you\\s+are\\s+now", Pattern.CASE_INSENSITIVE)
    );

    public void inject(List<LlmMessage> messages, Long agentId) {
        log.info("Injecting context for agent {}", agentId);
        // Load team context, knowledge docs, memory summaries
        // Prepend system context message if needed
    }

    public void injectVariables(List<LlmMessage> messages, Map<String, Object> variables) {
        if (variables == null || variables.isEmpty()) {
            return;
        }
        for (LlmMessage message : messages) {
            if (message.getContent() != null) {
                String content = message.getContent();
                for (Map.Entry<String, Object> entry : variables.entrySet()) {
                    String sanitizedValue = sanitize(String.valueOf(entry.getValue()));
                    content = content.replace("{{" + entry.getKey() + "}}", sanitizedValue);
                }
                message.setContent(content);
            }
        }
    }

    public String sanitize(String content) {
        if (content == null) {
            return "";
        }
        String sanitized = content
            .replace("<", "&lt;")
            .replace(">", "&gt;");

        for (Pattern pattern : DANGEROUS_PATTERNS) {
            if (pattern.matcher(sanitized).find()) {
                log.warn("Potential prompt injection detected in content");
                return "[CONTENT_FILTERED]";
            }
        }

        return sanitized;
    }
}
