package com.schemaplexai.task.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * Request body of {@code POST /task/tasks/{taskId}/comments}.
 */
@Data
public class CommentCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "content is required")
    private String content;
}
