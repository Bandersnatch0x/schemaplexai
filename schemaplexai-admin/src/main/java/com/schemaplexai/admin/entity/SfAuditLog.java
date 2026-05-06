package com.schemaplexai.admin.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.schemaplexai.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_audit_log")
public class SfAuditLog extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableField("tenant_id")
    private String tenantId;

    @TableField("user_id")
    private Long userId;

    @TableField("username")
    private String username;

    @TableField("action")
    private String action;

    @TableField("resource_type")
    private String resourceType;

    @TableField("resource_id")
    private String resourceId;

    @TableField("details")
    private String details;

    @TableField("ip_address")
    private String ipAddress;

    @TableField("user_agent")
    private String userAgent;

    @TableField("status")
    private Integer status;

    @TableField("error_message")
    private String errorMessage;

    @TableField("executed_at")
    private LocalDateTime executedAt;
}
