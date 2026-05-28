package com.schemaplexai.agent.engine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@TableName("sf_execution_event")
public class ExecutionEvent {

    @TableId(value = "event_id", type = IdType.INPUT)
    private UUID eventId;
    private Long executionId;
    private Integer seq;
    private String eventType;
    private String payload;
    private Instant occurredAt;
    private Long tenantId;
    private Long agentId;
    private String sensitivity;
}
