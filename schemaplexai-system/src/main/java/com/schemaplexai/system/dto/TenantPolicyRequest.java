package com.schemaplexai.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO for creating or updating a tenant policy.
 */
@Data
public class TenantPolicyRequest {

    @NotBlank(message = "configJson must not be blank")
    private String configJson;
}
