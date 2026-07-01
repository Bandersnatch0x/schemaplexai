package com.schemaplexai.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * M6.4: Escalation request DTO for escalate operations.
 */
@Data
public class EscalationRequest {

    @NotBlank(message = "escalatorId must not be blank")
    private String escalatorId;

    private String reason;
}
