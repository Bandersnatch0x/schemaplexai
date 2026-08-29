package com.schemaplexai.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * M6.4: Approval request DTO for approve/reject operations.
 */
@Data
public class ApprovalRequest {

    @NotBlank(message = "approverId must not be blank")
    private String approverId;

    @NotBlank(message = "reason must not be blank")
    private String reason;

    private String comment;
}
