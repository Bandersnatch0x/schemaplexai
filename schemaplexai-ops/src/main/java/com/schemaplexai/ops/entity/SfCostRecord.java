package com.schemaplexai.ops.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.schemaplexai.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * PostgreSQL entity for cost records (v1 short-path).
 * Mirrors {@link CostRecord} but maps to PG table {@code sf_cost_record} via MyBatis-Plus.
 * v1.1 will migrate to ClickHouse for time-series analytics.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_cost_record")
public class SfCostRecord extends BaseEntity {

    private String recordId;
    private String serviceName;
    private String modelName;
    private String provider;
    private String requestType;
    private Long inputTokens;
    private Long outputTokens;
    private Long totalTokens;
    private BigDecimal costAmount;
    private String currency;
    private LocalDateTime occurredAt;
    private Long executionId;
    private Long agentId;
    private Long workflowInstanceId;
}
