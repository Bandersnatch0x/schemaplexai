package com.schemaplexai.admin.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SystemConfigDTO {

    private Long id;
    private String tenantId;
    private String configKey;
    private String configValue;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
