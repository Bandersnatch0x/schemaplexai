package com.schemaplexai.agent.engine.cost;

import com.schemaplexai.agent.engine.mq.CostRecordedEventPublisher;
import com.schemaplexai.model.event.CostRecordedEvent;
import dev.langchain4j.model.output.TokenUsage;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Token usage collection point required by the cost-analytics spec §1/§2:
 * records the input/output token counts of every LLM call and emits a
 * {@link CostRecordedEvent} into the cost pipeline.
 *
 * <p>The LangChain4j {@code Response.tokenUsage()} returned by providers was
 * previously discarded (see spec-compliance review REQ-01). Providers now
 * report each completed call through {@link LlmUsageDispatch}; this recorder
 * joins the usage with the {@link LlmCallContext} bound by the state machine
 * and publishes the event on {@code sf.exchange} / {@code sf.cost}.
 *
 * <p>Pricing is intentionally NOT computed here: the consuming CostService
 * (schemaplexai-ops) owns model pricing configuration and calculates the cost
 * authoritatively when persisting the record. The event therefore carries
 * {@code costAmount=null} and the default currency code.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenUsageRecorder {

    /** Request type stamped on LLM chat-completion cost events. */
    static final String REQUEST_TYPE_CHAT = "chat";
    /** Default currency for cost events (spec §3.1: USD). */
    static final String DEFAULT_CURRENCY = "USD";

    private final CostRecordedEventPublisher costRecordedEventPublisher;

    @PostConstruct
    void register() {
        LlmUsageDispatch.setListener(this::onTokenUsage);
    }

    @PreDestroy
    void unregister() {
        LlmUsageDispatch.setListener(null);
    }

    /**
     * Handles a completed LLM call: joins the token usage with the bound
     * execution context and publishes a cost-recorded event.
     *
     * <p>Skips silently (debug level) when no execution context is bound —
     * e.g. exploration/experimentation calls outside agent execution.
     *
     * @param providerName provider that served the call (e.g. OPENAI)
     * @param modelId      resolved model identifier actually used
     * @param tokenUsage   token usage reported by the LLM response
     */
    public void onTokenUsage(String providerName, String modelId, TokenUsage tokenUsage) {
        if (tokenUsage == null
                || (tokenUsage.inputTokenCount() == null && tokenUsage.outputTokenCount() == null)) {
            log.debug("No token usage reported for provider={}, model={}; cost event skipped",
                    providerName, modelId);
            return;
        }

        LlmCallContext context = LlmCallContext.current();
        if (context == null || context.executionId() == null) {
            log.debug("LLM call outside an execution context (provider={}, model={}); "
                    + "cost event skipped", providerName, modelId);
            return;
        }

        Long tenantId = parseTenantId(context.tenantId());
        if (tenantId == null) {
            log.warn("Cannot attribute cost event: tenantId '{}' of execution {} is not numeric",
                    context.tenantId(), context.executionId());
            return;
        }

        long inputTokens = tokenUsage.inputTokenCount() != null
                ? tokenUsage.inputTokenCount().longValue() : 0L;
        long outputTokens = tokenUsage.outputTokenCount() != null
                ? tokenUsage.outputTokenCount().longValue() : 0L;
        long totalTokens = tokenUsage.totalTokenCount() != null
                ? tokenUsage.totalTokenCount().longValue() : inputTokens + outputTokens;

        CostRecordedEvent event = new CostRecordedEvent(
                UUID.randomUUID(),
                context.executionId(),
                tenantId,
                context.agentId(),
                modelId,
                providerName,
                REQUEST_TYPE_CHAT,
                inputTokens,
                outputTokens,
                totalTokens,
                null,
                DEFAULT_CURRENCY,
                Instant.now());

        costRecordedEventPublisher.publishCostRecorded(event);
    }

    private Long parseTenantId(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(tenantId.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
