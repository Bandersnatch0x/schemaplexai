package com.schemaplexai.agent.engine.context;

import com.schemaplexai.agent.engine.model.LlmMessage;
import com.schemaplexai.agent.engine.tool.ValidationResult;
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

    private final List<InputValidator> validators;

    public ContextInjector(List<InputValidator> validators) {
        this.validators = validators;
    }

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
                // Validate input before variable substitution
                validateInput(message.getContent());

                String content = message.getContent();
                for (Map.Entry<String, Object> entry : variables.entrySet()) {
                    String sanitizedValue = sanitize(String.valueOf(entry.getValue()));
                    content = content.replace("{{" + entry.getKey() + "}}", sanitizedValue);
                }

                // Validate output after variable substitution
                validateInput(content);
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

    /**
     * 使用所有注册的 InputValidator 验证输入
     * @param input 待验证文本
     * @throws IllegalArgumentException 验证失败时抛出
     */
    public void validateInput(String input) {
        for (InputValidator validator : validators) {
            ValidationResult result = validator.validate(input);
            if (!result.isValid()) {
                throw new IllegalArgumentException("Input validation failed: " + result.errorMessage());
            }
        }
    }
}
