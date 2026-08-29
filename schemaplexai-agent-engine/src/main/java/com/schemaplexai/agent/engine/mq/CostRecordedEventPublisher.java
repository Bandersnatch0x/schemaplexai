package com.schemaplexai.agent.engine.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.common.constants.CommonConstants;
import com.schemaplexai.model.event.CostRecordedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes {@link CostRecordedEvent}s to the platform event bus.
 *
 * <p>Producer contract: {@code src/test/resources/contracts/costRecordedEvent.groovy}
 * — every LLM call's token usage is emitted as a CostRecordedEvent on routing
 * key {@code sf.cost} with header {@code eventType=CostRecordedEvent}. The
 * {@code sf.cost.queue} consumer (schemaplexai-task CostSyncConsumer) persists
 * the record to PG {@code sf_cost_record} via ops CostService.
 *
 * <p>Publishing follows the same convention as {@link AgentExecutionEventPublisher}
 * (shared {@code sf.exchange} + CommonConstants routing keys). Cost telemetry is
 * best-effort: publish failures are logged and swallowed so a degraded MQ never
 * fails the agent execution that produced the usage.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CostRecordedEventPublisher {

    /** Message header identifying the payload shape for consumers. */
    public static final String EVENT_TYPE_HEADER = "eventType";
    /** Header value carried by cost-recorded events. */
    public static final String EVENT_TYPE_COST_RECORDED = "CostRecordedEvent";

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Publishes a cost-recorded event to {@code sf.exchange} / {@code sf.cost}.
     *
     * @param event the cost event (must not be null)
     */
    public void publishCostRecorded(CostRecordedEvent event) {
        if (event == null) {
            log.warn("Ignoring null CostRecordedEvent");
            return;
        }
        try {
            String payload = objectMapper.writeValueAsString(event);
            rabbitTemplate.convertAndSend(
                    CommonConstants.EXCHANGE_SCHEMAPLEXAI,
                    CommonConstants.RK_COST,
                    payload,
                    message -> {
                        message.getMessageProperties().setHeader(
                                EVENT_TYPE_HEADER, EVENT_TYPE_COST_RECORDED);
                        message.getMessageProperties().setContentType("application/json");
                        return message;
                    });
            log.info("Published cost recorded event: executionId={}, model={}, in={}, out={}",
                    event.executionId(), event.modelName(), event.inputTokens(), event.outputTokens());
        } catch (Exception e) {
            // Cost telemetry must never fail the execution that produced it.
            log.error("Failed to publish cost recorded event for execution {}: {}",
                    event.executionId(), e.getMessage(), e);
        }
    }
}
