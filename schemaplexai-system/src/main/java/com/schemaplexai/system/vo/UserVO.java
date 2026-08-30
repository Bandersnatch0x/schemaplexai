package com.schemaplexai.system.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * View object for SfUser that deliberately omits the password hash field.
 */
@Data
public class UserVO {

    private Long id;
    private String tenantId;
    private String username;
    private String email;
    private String phone;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
