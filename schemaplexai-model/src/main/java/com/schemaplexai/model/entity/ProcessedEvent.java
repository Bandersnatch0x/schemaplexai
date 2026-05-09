package com.schemaplexai.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

/**
 * Processed event tracking for consumer idempotency.
 * Uses composite PK (event_id, consumer_name) so each consumer independently tracks deduplication.
 */
@Data
@TableName("sf_processed_event")
public class ProcessedEvent {

    private UUID eventId;
    private String consumerName;
    private Instant processedAt;
}