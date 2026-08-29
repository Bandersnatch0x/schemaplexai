package com.schemaplexai.system.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO for assigning a permission to a role.
 */
@Data
public class PermissionAssignmentRequest {

    @NotNull(message = "roleId must not be null")
    private Long roleId;

    @NotNull(message = "permissionId must not be null")
    private Long permissionId;
}
