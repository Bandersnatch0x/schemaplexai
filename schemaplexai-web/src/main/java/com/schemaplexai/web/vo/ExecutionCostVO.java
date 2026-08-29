package com.schemaplexai.web.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * M6.4: Per-execution cost breakdown value object for HTTP API responses.
 */
@Data
public class ExecutionCostVO {

    private Long executionId;

    private Long tenantId;

    private BigDecimal totalCost;

    private String currency;

    private Long inputTokens;

    private Long outputTokens;

    private Long totalTokens;

    private Integer recordCount;
}
