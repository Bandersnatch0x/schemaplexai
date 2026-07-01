package com.schemaplexai.web.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * M6.4: Execution status value object for HTTP API responses.
 */
@Data
public class ExecutionStatusVO {

    private Long executionId;

    private Long agentId;

    private String agentName;

    private String state;

    private Integer currentRound;

    private Long consumedTokens;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
