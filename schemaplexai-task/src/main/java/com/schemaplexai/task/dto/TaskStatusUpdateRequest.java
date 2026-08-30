package com.schemaplexai.task.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * Request body of {@code PUT /task/tasks/{id}/status}.
 */
@Data
public class TaskStatusUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "status is required")
    private String status;

    /** Required when moving to BLOCKED. */
    private String blockerReason;
}
