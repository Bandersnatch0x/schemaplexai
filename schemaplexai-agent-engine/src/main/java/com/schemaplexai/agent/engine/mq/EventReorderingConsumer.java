package com.schemaplexai.agent.engine.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.schemaplexai.agent.engine.entity.ExecutionEvent;
import com.schemaplexai.agent.engine.job.GapRecoveryJob;
import com.schemaplexai.agent.engine.service.ExecutionEventBuffer;
import com.schemaplexai.agent.engine.service.ExecutionEventService;
import com.schemaplexai.model.event.ExecutionEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * MQ consumer that buffers out-of-order execution events and applies them in seq order.
 * <p>
 * Each execution maintains its own buffer and confirmed-seq watermark.
 * Events arriving out of order are held until the missing seq arrives.
 * Gap detection triggers {@link GapRecoveryJob} for automatic repair when enabled.
 * <p>
 * Idempotent: duplicate eventIds are silently ignored (UUID PK on sf_execution_event).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventReorderingConsumer {

    private static final String QUEUE_NAME = "sf.execution.events.reorder.queue";
    private static final String EXCHANGE_NAME = "execution_events";

    private final ExecutionEventBuffer eventBuffer;
    private final ExecutionEventService executionEventService;
    private final ObjectProvider<GapRecoveryJob> gapRecoveryJobProvider;
    private final ObjectMapper objectMapper;

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(value = QUEUE_NAME, durable = "true"),
                    exchange = @Exchange(value = EXCHANGE_NAME, type = "topic"),
                    key = "execution.#"
            )
    )
    public void onMessage(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String body = new String(message.getBody(), StandardCharsets.UTF_8);

        try {
            ExecutionEventMessage msg = objectMapper.readValue(body, ExecutionEventMessage.class);

            if (msg.executionId() == null) {
                log.warn("[EventReorderingConsumer] Missing executionId, skipping: {}", body);
                channel.basicAck(deliveryTag, false);
                return;
            }

            ExecutionEvent event = convert(msg);
            List<ExecutionEvent> ready = eventBuffer.applyInOrder(msg.executionId(), List.of(event));

            for (ExecutionEvent e : ready) {
                applyEvent(e);
            }

            // Gap detection and recovery
            if (eventBuffer.hasGap(msg.executionId())) {
                log.warn("[EventReorderingConsumer] Gap detected for execution={}, nextExpectedSeq={}, buffered={}",
                        msg.executionId(),
                        eventBuffer.getNextExpectedSeq(msg.executionId()),
                        eventBuffer.getBufferedCount(msg.executionId()));
                triggerGapRecovery(msg.executionId());
            }

            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("[EventReorderingConsumer] Failed to process message: {}", body, e);
            channel.basicNack(deliveryTag, false, false);
        }
    }

    private void applyEvent(ExecutionEvent event) throws Exception {
        try {
            executionEventService.writeEvent(event);
            log.debug("[EventReorderingConsumer] Applied ordered event: execution={}, seq={}, type={}",
                    event.getExecutionId(), event.getSeq(), event.getEventType());
        } catch (DuplicateKeyException e) {
            log.debug("[EventReorderingConsumer] Event already exists (idempotent skip): eventId={}",
                    event.getEventId());
        } catch (Exception e) {
            log.error("[EventReorderingConsumer] Failed to apply event: execution={}, seq={}",
                    event.getExecutionId(), event.getSeq(), e);
            throw e;
        }
    }

    private void triggerGapRecovery(Long executionId) {
        GapRecoveryJob gapRecoveryJob = gapRecoveryJobProvider.getIfAvailable();
        if (gapRecoveryJob == null) {
            log.warn("[EventReorderingConsumer] Gap recovery is disabled; skipping recovery for execution={}",
                    executionId);
            return;
        }
        gapRecoveryJob.recoverGapsForExecution(executionId);
    }

    private ExecutionEvent convert(ExecutionEventMessage msg) {
        ExecutionEvent event = new ExecutionEvent();
        event.setEventId(msg.eventId());
        event.setExecutionId(msg.executionId());
        event.setSeq(msg.seq());
        event.setEventType(msg.eventType());
        event.setPayload(msg.payload());
        event.setOccurredAt(msg.occurredAt());
        event.setTenantId(msg.tenantId());
        event.setAgentId(msg.agentId());
        event.setSensitivity(msg.sensitivity());
        return event;
    }
}
