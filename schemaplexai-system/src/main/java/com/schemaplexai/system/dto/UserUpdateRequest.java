package com.schemaplexai.system.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO for updating an existing user via the REST API.
 * Password update goes through the dedicated change-password endpoint.
 */
@Data
public class UserUpdateRequest {

    @Size(min = 3, max = 50, message = "username must be 3-50 characters")
    private String username;

    @Size(max = 100, message = "email must not exceed 100 characters")
    private String email;

    @Size(max = 20, message = "phone must not exceed 20 characters")
    private String phone;

    private String status;
}
