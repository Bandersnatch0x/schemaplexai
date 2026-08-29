package com.schemaplexai.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO for creating a new user via the REST API.
 */
@Data
public class UserCreateRequest {

    @NotBlank(message = "username must not be blank")
    @Size(min = 3, max = 50, message = "username must be 3-50 characters")
    private String username;

    @NotBlank(message = "password must not be blank")
    @Size(min = 6, max = 100, message = "password must be 6-100 characters")
    private String password;

    @Size(max = 100, message = "email must not exceed 100 characters")
    private String email;

    @Size(max = 20, message = "phone must not exceed 20 characters")
    private String phone;

    private Integer status;

    private String tenantId;
}
