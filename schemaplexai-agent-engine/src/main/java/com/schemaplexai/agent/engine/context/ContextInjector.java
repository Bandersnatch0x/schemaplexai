package com.schemaplexai.agent.engine.context;

import com.schemaplexai.agent.engine.model.LlmMessage;
import com.schemaplexai.agent.engine.rag.MilvusIsolationService;
import com.schemaplexai.agent.engine.rag.SearchResult;
import com.schemaplexai.common.context.TenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.SocketTimeoutException;
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

    private static final int DEFAULT_MAX_RETRIES = 2;
    private static final long DEFAULT_RETRY_DELAY_MS = 200;

    private MilvusIsolationService ragService;
    private EmbeddingService embeddingService;
    private final int maxRetries;
    private final long retryDelayMs;

    public ContextInjector() {
        this.maxRetries = DEFAULT_MAX_RETRIES;
        this.retryDelayMs = DEFAULT_RETRY_DELAY_MS;
    }

    public ContextInjector(MilvusIsolationService ragService, EmbeddingService embeddingService) {
        this(ragService, embeddingService, DEFAULT_MAX_RETRIES, DEFAULT_RETRY_DELAY_MS);
    }

    public ContextInjector(MilvusIsolationService ragService, EmbeddingService embeddingService,
                           int maxRetries, long retryDelayMs) {
        this.ragService = ragService;
        this.embeddingService = embeddingService;
        this.maxRetries = Math.max(0, maxRetries);
        this.retryDelayMs = Math.max(0, retryDelayMs);
    }

    public void setRagService(MilvusIsolationService ragService) {
        this.ragService = ragService;
    }

    public void setEmbeddingService(EmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
    }

    public void inject(List<LlmMessage> messages, Long agentId) {
        log.info("Injecting context for agent {}", agentId);
        if (messages == null || messages.isEmpty()) {
            return;
        }

        String latestUserPrompt = extractLatestUserPrompt(messages);
        if (latestUserPrompt == null || latestUserPrompt.isBlank()) {
            log.debug("No user prompt found for RAG injection, agentId={}", agentId);
            return;
        }

        String tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            log.debug("No tenantId in context, skipping RAG injection for agentId={}", agentId);
            return;
        }

        AgentContext agentContext = AgentContext.builder()
                .agentId(agentId)
                .tenantId(tenantId)
                .build();

        try {
            String ragContext = retrieveRagContext(latestUserPrompt, agentContext);
            if (ragContext != null && !ragContext.isBlank()) {
                insertRagSystemMessage(messages, ragContext);
            }
        } catch (Exception e) {
            log.warn("RAG context injection failed for agentId={}, proceeding with original messages", agentId, e);
        }
    }

    private String extractLatestUserPrompt(List<LlmMessage> messages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            LlmMessage msg = messages.get(i);
            if ("user".equals(msg.getRole()) && msg.getContent() != null) {
                return msg.getContent();
            }
        }
        return null;
    }

    private void insertRagSystemMessage(List<LlmMessage> messages, String ragContext) {
        int lastUserIndex = -1;
        for (int i = messages.size() - 1; i >= 0; i--) {
            if ("user".equals(messages.get(i).getRole())) {
                lastUserIndex = i;
                break;
            }
        }
        if (lastUserIndex < 0) {
            return;
        }

        String systemContent = "Context from knowledge base:\n" + ragContext;
        messages.add(lastUserIndex, new LlmMessage("system", systemContent));
    }

    /**
     * Inject RAG-retrieved context into the prompt.
     * RAG failures are non-blocking: if retrieval fails, the original prompt is returned.
     *
     * @param prompt  user prompt
     * @param context agent context with tenant/project info
     * @return enriched prompt with knowledge-base context
     */
    public String injectWithContext(String prompt, AgentContext context) {
        if (context == null || context.getTenantId() == null) {
            return prompt;
        }

        String ragContext = retrieveRagContext(prompt, context);
        return combineContexts(prompt, ragContext);
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

    private void validateInput(String content) {
        if (content == null) return;
        for (Pattern pattern : DANGEROUS_PATTERNS) {
            if (pattern.matcher(content).find()) {
                log.warn("Potential prompt injection detected during validation");
                throw new IllegalArgumentException("Input contains potentially dangerous content");
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

    private String retrieveRagContext(String prompt, AgentContext context) {
        if (ragService == null || embeddingService == null) {
            log.debug("RAG services not available, skipping context retrieval");
            return "";
        }

        int attempt = 0;
        while (true) {
            attempt++;
            try {
                float[] queryEmbedding = embeddingService.embed(prompt);
                List<SearchResult> results = ragService.searchWithIsolation(
                        context.getTenantId(),
                        context.getProjectId(),
                        queryEmbedding
                );
                return formatSearchResults(results);
            } catch (Exception e) {
                if (isRetryable(e) && attempt <= maxRetries) {
                    log.warn("RAG retrieval failed for tenant={} on attempt {}/{}, retrying after {}ms",
                            context.getTenantId(), attempt, maxRetries + 1, retryDelayMs, e);
                    sleepQuietly(retryDelayMs);
                } else {
                    log.warn("RAG retrieval failed for tenant={} on attempt {}, proceeding without context",
                            context.getTenantId(), attempt, e);
                    return "";
                }
            }
        }
    }

    /**
     * Determine if an exception represents a transient failure that may succeed on retry.
     */
    boolean isRetryable(Exception e) {
        if (e instanceof SocketTimeoutException) {
            return true;
        }
        String message = e.getMessage();
        if (message != null && isTransientMessage(message)) {
            return true;
        }
        Throwable cause = e.getCause();
        if (cause instanceof SocketTimeoutException) {
            return true;
        }
        if (cause != null && cause.getMessage() != null && isTransientMessage(cause.getMessage())) {
            return true;
        }
        return false;
    }

    private boolean isTransientMessage(String message) {
        String lower = message.toLowerCase();
        return lower.contains("timeout")
                || lower.contains("timed out")
                || lower.contains("transient")
                || lower.contains("temporarily unavailable")
                || lower.contains("connection reset")
                || lower.contains("broken pipe")
                || lower.contains("too many requests")
                || lower.contains("rate limit");
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private String combineContexts(String prompt, String ragContext) {
        if (ragContext == null || ragContext.isBlank()) {
            return prompt;
        }
        return "Context from knowledge base:\n" + ragContext + "\n\nUser query:\n" + prompt;
    }

    private String formatSearchResults(List<SearchResult> results) {
        if (results == null || results.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < results.size(); i++) {
            SearchResult result = results.get(i);
            sb.append(i + 1).append(". ").append(result.getContent()).append("\n");
            if (result.getSource() != null) {
                sb.append("   Source: ").append(result.getSource()).append("\n");
            }
        }
        return sb.toString();
    }
}
