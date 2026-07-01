package com.schemaplexai.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO for the change-password endpoint.
 */
@Data
public class ChangePasswordRequest {

    @NotBlank(message = "oldPassword must not be blank")
    private String oldPassword;

    @NotBlank(message = "newPassword must not be blank")
    @Size(min = 6, max = 100, message = "newPassword must be 6-100 characters")
    private String newPassword;
}
