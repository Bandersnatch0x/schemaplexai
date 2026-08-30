package com.schemaplexai.task.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.schemaplexai.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * Task board item (DDL {@code sf_task} in {@code 03-init-schema-others.sql}),
 * backing the {@code /task/tasks} REST endpoints consumed by the frontend
 * task board ({@code schemaplexai-ui/src/api/task.ts}).
 *
 * <p>Status values follow the frontend contract:
 * BACKLOG / QUEUED / IN_PROGRESS / AWAITING_REVIEW / REVISING / BLOCKED / DONE.
 * Priority values: P0..P3. Assignment types: MANUAL / AUTO / MIXED.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "sf_task", autoResultMap = true)
public class SfTask extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private String title;

    private String description;

    /** Skill tags, serialized as a JSON array string in {@code skill_tags}. */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> skillTags;

    /** P0 (highest) .. P3. */
    private String priority;

    /** Board column, one of the status values documented on the class. */
    private String status;

    private String assignedRuntimeId;

    private Long assignedAgentId;

    /** MANUAL / AUTO / MIXED. */
    private String assignmentType;

    private Long specId;

    /** Mandatory while {@code status = BLOCKED}. */
    private String blockerReason;
}
