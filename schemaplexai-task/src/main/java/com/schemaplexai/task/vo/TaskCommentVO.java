package com.schemaplexai.task.vo;

import com.schemaplexai.task.entity.SfTaskComment;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * View of a task comment, matching {@code TaskComment} in
 * {@code schemaplexai-ui/src/api/task.ts}.
 */
@Data
public class TaskCommentVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String taskId;
    private String content;
    private String authorId;
    private String authorName;
    private LocalDateTime createdAt;

    public static TaskCommentVO from(SfTaskComment comment) {
        TaskCommentVO vo = new TaskCommentVO();
        vo.setId(comment.getId() == null ? null : String.valueOf(comment.getId()));
        vo.setTaskId(comment.getTaskId() == null ? null : String.valueOf(comment.getTaskId()));
        vo.setContent(comment.getContent());
        vo.setAuthorId(comment.getAuthorId() == null ? null : String.valueOf(comment.getAuthorId()));
        vo.setAuthorName(comment.getAuthorName());
        vo.setCreatedAt(comment.getCreatedAt());
        return vo;
    }
}
