package com.schemaplexai.agent.engine.cost;

import dev.langchain4j.model.output.TokenUsage;
import lombok.extern.slf4j.Slf4j;

/**
 * Static dispatch point for LLM token-usage reports.
 *
 * <p>{@link com.schemaplexai.agent.engine.model.LlmProviderAdapter} instances are
 * created without Spring-managed collaborators (subclasses only receive their
 * provider properties), so usage reporting is routed through this static hook.
 * The {@link TokenUsageRecorder} bean registers itself here at startup; when no
 * recorder is registered (plain unit tests, contexts without cost wiring) usage
 * reports are silently dropped.
 *
 * <p>Reporting is defensive on purpose: a failure in cost telemetry must never
 * fail the underlying LLM call.
 */
@Slf4j
public final class LlmUsageDispatch {

    /**
     * Receives token-usage observations for completed LLM calls.
     */
    @FunctionalInterface
    public interface Listener {
        void onTokenUsage(String providerName, String modelId, TokenUsage tokenUsage);
    }

    private static volatile Listener listener;

    private LlmUsageDispatch() {
        // static dispatch utility
    }

    /**
     * Registers the usage listener (typically the TokenUsageRecorder bean).
     * Passing null clears the registration.
     */
    public static void setListener(Listener newListener) {
        listener = newListener;
    }

    /**
     * Reports a completed LLM call's token usage to the registered listener.
     * Never throws: listener failures are logged and swallowed so cost
     * telemetry cannot break agent execution.
     *
     * @param providerName the provider that served the call (e.g. OPENAI)
     * @param modelId      the resolved model identifier actually used
     * @param tokenUsage   LangChain4j token usage (may be null — then no-op)
     */
    public static void report(String providerName, String modelId, TokenUsage tokenUsage) {
        Listener current = listener;
        if (current == null || tokenUsage == null) {
            return;
        }
        try {
            current.onTokenUsage(providerName, modelId, tokenUsage);
        } catch (Exception e) {
            log.warn("Token usage listener failed for provider={}, model={}: {}",
                    providerName, modelId, e.getMessage(), e);
        }
    }
}
