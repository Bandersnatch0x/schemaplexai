package com.schemaplexai.admin.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserAdminDTO {

    private Long id;
    private String tenantId;
    private String username;
    private String email;
    private String phone;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<String> roles;
    private Long auditLogCount;
    private LocalDateTime lastLoginAt;
}
