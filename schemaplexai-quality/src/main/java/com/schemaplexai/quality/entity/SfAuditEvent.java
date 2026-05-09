package com.schemaplexai.quality.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.schemaplexai.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_audit_event")
public class SfAuditEvent extends BaseEntity {

    private String eventType;
    private String resourceType;
    private Long resourceId;
    private String action;
    private String detailsJson;
    private Long userId;

    // --- M4.4 Audit Trail Projection extensions ---
    private Long executionId;
    private UUID eventId;
    private LocalDateTime occurredAt;
    private String contentHash;
    private Boolean corrupted;
}
