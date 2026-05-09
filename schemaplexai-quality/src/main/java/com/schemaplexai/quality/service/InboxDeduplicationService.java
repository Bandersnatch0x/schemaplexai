package com.schemaplexai.quality.service;

import com.schemaplexai.quality.mapper.SfProcessedEventMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Inbox deduplication service (M6.5).
 * Tracks processed MQ events per consumer to ensure idempotent consumption.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InboxDeduplicationService {

    private final SfProcessedEventMapper processedEventMapper;

    /**
     * Checks whether the given event has already been processed by the specified consumer.
     *
     * @param eventId       the event identifier
     * @param consumerName  the consumer name
     * @return true if already processed, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean isProcessed(UUID eventId, String consumerName) {
        boolean processed = processedEventMapper.exists(eventId, consumerName);
        if (processed) {
            log.debug("[InboxDeduplication] Event already processed: eventId={}, consumer={}",
                    eventId, consumerName);
        }
        return processed;
    }

    /**
     * Marks the given event as processed by the specified consumer.
     *
     * @param eventId       the event identifier
     * @param consumerName  the consumer name
     */
    @Transactional(rollbackFor = Exception.class)
    public void markProcessed(UUID eventId, String consumerName) {
        processedEventMapper.insertProcessed(eventId, consumerName);
        log.debug("[InboxDeduplication] Marked event as processed: eventId={}, consumer={}",
                eventId, consumerName);
    }
}
