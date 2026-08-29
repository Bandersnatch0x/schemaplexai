package com.schemaplexai.web.vo;

import lombok.Data;

import java.time.Instant;

/**
 * M6.4: Approval ticket value object for HTTP API responses.
 */
@Data
public class ApprovalVO {

    private String ticketId;

    private Long executionId;

    private Long agentId;

    private String stage;

    private String handler;

    private String riskLevel;

    private String actionDescription;

    private Integer triggeringSeq;

    private Instant createdAt;

    private Instant updatedAt;
}
