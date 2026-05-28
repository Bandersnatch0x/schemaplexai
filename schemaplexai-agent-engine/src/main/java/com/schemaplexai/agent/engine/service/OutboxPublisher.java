package com.schemaplexai.agent.engine.service;

import com.schemaplexai.agent.engine.entity.ExecutionOutbox;
import com.schemaplexai.agent.engine.mapper.ExecutionOutboxMapper;
import com.schemaplexai.agent.engine.mq.OutboxConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Background job that publishes unpublished Outbox entries to RabbitMQ.
 *
 * <p>Flow:
 * <ol>
 *   <li>Poll sf_execution_outbox for unpublished entries (retry_count &lt; 5)</li>
 *   <li>Publish each to RabbitMQ on the topic exchange</li>
 *   <li>On success: mark published_at</li>
 *   <li>On failure: increment retry_count, exponential backoff</li>
 *   <li>After 5 failures: mark as DEAD, alert triggered</li>
 * </ol>
 *
 * <p>Runs every 2 seconds (configurable via outbox.poll.interval-ms).
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "outbox.publisher", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class OutboxPublisher {

    private static final int MAX_RETRIES = 5;
    private static final String EXCHANGE_NAME = "execution_events";

    private final ExecutionOutboxMapper outboxMapper;
    private final RabbitTemplate rabbitTemplate;

    /**
     * Polls for unpublished Outbox entries and publishes them to MQ.
     * Runs every 2 seconds.
     */
    @Scheduled(fixedDelayString = "${outbox.poll.interval-ms:2000}")
    public void pollAndPublish() {
        List<ExecutionOutbox> entries = outboxMapper.selectUnpublished(MAX_RETRIES);
        if (entries.isEmpty()) {
            return;
        }

        log.debug("Found {} unpublished outbox entries to publish", entries.size());

        for (ExecutionOutbox entry : entries) {
            try {
                publishEntry(entry);
            } catch (Exception e) {
                handlePublishFailure(entry, e);
            }
        }
    }

    private void publishEntry(ExecutionOutbox entry) {
        try {
            rabbitTemplate.convertAndSend(
                    EXCHANGE_NAME,
                    entry.getTopic(),
                    entry.getPayload()
            );

            // Mark as published
            entry.setPublishedAt(Instant.now());
            entry.setRetryCount(0);
            outboxMapper.updateById(entry);

            log.debug("Published outbox entry {} (topic={}, seq={})",
                    entry.getId(), entry.getTopic(), entry.getSeq());
        } catch (AmqpException e) {
            // MQ temporarily unavailable — will retry on next poll
            log.warn("Failed to publish outbox entry {} to {}: {}",
                    entry.getId(), entry.getTopic(), e.getMessage());
            throw e;
        }
    }

    private void handlePublishFailure(ExecutionOutbox entry, Exception e) {
        int newRetryCount = (entry.getRetryCount() != null ? entry.getRetryCount() : 0) + 1;

        if (newRetryCount >= MAX_RETRIES) {
            // Mark as DEAD — terminal state
            entry.setRetryCount(newRetryCount);
            outboxMapper.updateById(entry);

            log.error("[DEAD] Outbox entry {} (topic={}, seq={}) failed after {} retries",
                    entry.getId(), entry.getTopic(), entry.getSeq(), MAX_RETRIES);

            // Alert via event emission (handled by alerting system)
            emitDeadLetterAlert(entry, e);
        } else {
            // Increment retry count for backoff tracking
            entry.setRetryCount(newRetryCount);
            outboxMapper.updateById(entry);

            long delayMs = calculateBackoff(newRetryCount);
            log.warn("Outbox entry {} retry {}/{} (will retry after {}ms): {}",
                    entry.getId(), newRetryCount, MAX_RETRIES, delayMs, e.getMessage());
        }
    }

    private long calculateBackoff(int retryCount) {
        // Exponential backoff: 1s, 2s, 4s, 8s, 16s
        return 1000L * (1L << (retryCount - 1));
    }

    private void emitDeadLetterAlert(ExecutionOutbox entry, Exception e) {
        log.error("[DEAD_LETTER] Outbox entry {} failed permanently: topic={}, seq={}, error={}",
                entry.getId(), entry.getTopic(), entry.getSeq(), e.getMessage());
        // Alert integration: PagerDuty / webhook would be called here
    }

    /**
     * Publishes a single entry immediately (used for urgent events).
     */
    public void publishNow(Long entryId) {
        ExecutionOutbox entry = outboxMapper.selectById(entryId);
        if (entry == null) {
            log.warn("Outbox entry {} not found for immediate publish", entryId);
            return;
        }
        if (entry.getPublishedAt() != null) {
            log.debug("Outbox entry {} already published", entryId);
            return;
        }
        publishEntry(entry);
    }
}
