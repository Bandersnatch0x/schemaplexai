package com.schemaplexai.task.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.schemaplexai.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Comment attached to a task board item (DDL {@code sf_task_comment} in
 * {@code 03-init-schema-others.sql}), backing {@code /task/tasks/{id}/comments}.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_task_comment")
public class SfTaskComment extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private Long taskId;

    private String content;

    private Long authorId;

    /** Resolved from {@code sf_user.username} at creation time. */
    private String authorName;
}
