package com.schemaplexai.admin.dto;

import com.schemaplexai.common.page.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class AuditLogQuery extends PageParam {

    private String tenantId;
    private Long userId;
    private String action;
    private String resourceType;
    private Integer status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
