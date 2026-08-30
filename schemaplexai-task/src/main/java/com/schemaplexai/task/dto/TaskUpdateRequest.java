package com.schemaplexai.task.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Request body of {@code PUT /task/tasks/{id}} — every field optional
 * (frontend {@code Partial<CreateTaskPayload>}); only supplied fields change.
 */
@Data
public class TaskUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Size(max = 255, message = "title must not exceed 255 characters")
    private String title;

    private String description;

    private List<String> skillTags;

    private String priority;

    private String assignmentType;

    private Long specId;
}
