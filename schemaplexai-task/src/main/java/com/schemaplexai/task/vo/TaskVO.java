package com.schemaplexai.task.vo;

import com.schemaplexai.task.entity.SfTask;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * View of a task board item, shaped exactly like the frontend contract
 * ({@code SfTask} in {@code schemaplexai-ui/src/types/index.ts}). Numeric ids
 * are rendered as strings so snowflake/sequence values never lose precision in
 * JavaScript.
 */
@Data
public class TaskVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String tenantId;
    private String title;
    private String description;
    private List<String> skillTags;
    private String priority;
    private String status;
    private String assignedRuntimeId;
    private String assignedAgentId;
    private String assignmentType;
    private String specId;
    private String blockerReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static TaskVO from(SfTask task) {
        TaskVO vo = new TaskVO();
        vo.setId(asString(task.getId()));
        vo.setTenantId(task.getTenantId());
        vo.setTitle(task.getTitle());
        vo.setDescription(task.getDescription());
        vo.setSkillTags(task.getSkillTags());
        vo.setPriority(task.getPriority());
        vo.setStatus(task.getStatus());
        vo.setAssignedRuntimeId(task.getAssignedRuntimeId());
        vo.setAssignedAgentId(asString(task.getAssignedAgentId()));
        vo.setAssignmentType(task.getAssignmentType());
        vo.setSpecId(asString(task.getSpecId()));
        vo.setBlockerReason(task.getBlockerReason());
        vo.setCreatedAt(task.getCreatedAt());
        vo.setUpdatedAt(task.getUpdatedAt());
        return vo;
    }

    private static String asString(Long value) {
        return value == null ? null : String.valueOf(value);
    }
}
