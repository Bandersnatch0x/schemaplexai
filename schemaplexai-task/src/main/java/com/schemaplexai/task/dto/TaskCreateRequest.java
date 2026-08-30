package com.schemaplexai.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Request body of {@code POST /task/tasks} (frontend {@code CreateTaskPayload}).
 */
@Data
public class TaskCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "title is required")
    @Size(max = 255, message = "title must not exceed 255 characters")
    private String title;

    private String description;

    private List<String> skillTags;

    /** P0..P3; defaults to P2 when absent. */
    private String priority;

    /** MANUAL / AUTO / MIXED; defaults to MANUAL when absent. */
    private String assignmentType;

    private Long specId;
}
