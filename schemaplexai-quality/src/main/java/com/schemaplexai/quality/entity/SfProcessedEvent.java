package com.schemaplexai.quality.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Tracks processed MQ events per consumer for inbox deduplication (M6.5).
 * Composite primary key: (eventId, consumerName).
 * Does NOT extend BaseEntity — this table manages its own identity.
 */
@Data
@TableName("sf_processed_event")
public class SfProcessedEvent {

    private UUID eventId;
    private String consumerName;
    private LocalDateTime processedAt;
}
