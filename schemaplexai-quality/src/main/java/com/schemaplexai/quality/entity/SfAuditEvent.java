package com.schemaplexai.quality.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.schemaplexai.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

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
}
