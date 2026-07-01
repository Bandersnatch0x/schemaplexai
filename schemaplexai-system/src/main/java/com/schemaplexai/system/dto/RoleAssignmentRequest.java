package com.schemaplexai.system.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO for assigning a role to a user.
 */
@Data
public class RoleAssignmentRequest {

    @NotNull(message = "userId must not be null")
    private Long userId;

    @NotNull(message = "roleId must not be null")
    private Long roleId;
}
