package com.schemaplexai.admin.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class RoleAdminDTO {

    private Long id;
    private String tenantId;
    private String name;
    private String code;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<String> permissions;
    private Long userCount;
}
