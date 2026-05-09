package com.schemaplexai.agent.engine.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@TableName("sf_execution_outbox")
public class ExecutionOutbox {

    private Long id;
    private UUID eventId;
    private Long executionId;
    private Integer seq;
    private String topic;
    private String payload;
    private Instant createdAt;
    private Instant publishedAt;
    private Integer retryCount;
}
