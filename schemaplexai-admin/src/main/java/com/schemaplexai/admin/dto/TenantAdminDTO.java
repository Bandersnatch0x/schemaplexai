package com.schemaplexai.admin.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TenantAdminDTO {

    private Long id;
    private String name;
    private String code;
    private Integer status;
    private String configJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Long userCount;
    private Long auditLogCount;
    private Long activeUserCountLast7Days;
}
