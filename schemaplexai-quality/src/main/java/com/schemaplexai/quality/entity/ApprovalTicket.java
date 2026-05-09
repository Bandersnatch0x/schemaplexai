package com.schemaplexai.quality.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@TableName("sf_approval_ticket")
public class ApprovalTicket {

    private Long id;
    private UUID ticketId;
    private Long executionId;
    private Long tenantId;
    private Long agentId;
    private UUID approvalRequestId;
    private String stage;
    private String handler;
    private String riskLevel;
    private String actionDescription;
    private Integer triggeringSeq;
    private Boolean deferred;
    private Instant decidedAt;
    private Integer decisionVersion;
    private Integer expectedExecutionVersion;
    private Instant createdAt;
    private Instant updatedAt;
}
