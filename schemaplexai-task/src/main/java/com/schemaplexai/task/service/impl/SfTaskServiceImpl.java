package com.schemaplexai.task.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.schemaplexai.common.context.TenantContextHolder;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.task.domain.TaskBoardValues;
import com.schemaplexai.task.dto.TaskCreateRequest;
import com.schemaplexai.task.dto.TaskStatusUpdateRequest;
import com.schemaplexai.task.dto.TaskUpdateRequest;
import com.schemaplexai.task.entity.SfTask;
import com.schemaplexai.task.mapper.SfTaskMapper;
import com.schemaplexai.task.service.SfTaskService;
import com.schemaplexai.task.vo.TaskPageResult;
import com.schemaplexai.task.vo.TaskVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Task board item service implementation backing {@code /task/tasks}.
 *
 * <p>Tenant isolation is enforced by the {@code TenantLineInnerInterceptor}
 * registered in {@code MyBatisPlusConfig}: every select/update/delete issued
 * here is automatically scoped to the tenant lifted from the gateway headers.
 * On create the tenant id is also stamped explicitly so the row is never
 * written without one.
 */
@Slf4j
@Service
public class SfTaskServiceImpl extends ServiceImpl<SfTaskMapper, SfTask> implements SfTaskService {

    private static final int MAX_PAGE_SIZE = 1000;

    @Override
    public TaskPageResult listTasks(int page, int pageSize, String status, String priority, String keyword) {
        long current = Math.max(page, 1);
        long size = Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
        if (status != null && !status.isBlank() && !TaskBoardValues.STATUSES.contains(status)) {
            throw new BaseException(ResultCode.PARAM_ERROR, "unknown task status: " + status);
        }
        if (priority != null && !priority.isBlank() && !TaskBoardValues.PRIORITIES.contains(priority)) {
            throw new BaseException(ResultCode.PARAM_ERROR, "unknown task priority: " + priority);
        }

        Page<SfTask> pageResult = lambdaQuery()
                .eq(status != null && !status.isBlank(), SfTask::getStatus, status)
                .eq(priority != null && !priority.isBlank(), SfTask::getPriority, priority)
                .and(keyword != null && !keyword.isBlank(),
                        w -> w.like(SfTask::getTitle, keyword.trim()).or().like(SfTask::getDescription, keyword.trim()))
                .orderByDesc(SfTask::getCreatedAt)
                .page(new Page<>(current, size));

        List<TaskVO> list = new ArrayList<>(pageResult.getRecords().size());
        for (SfTask task : pageResult.getRecords()) {
            list.add(TaskVO.from(task));
        }
        return new TaskPageResult(list, pageResult.getTotal());
    }

    @Override
    public SfTask createTask(TaskCreateRequest request) {
        SfTask task = new SfTask();
        task.setTitle(request.getTitle().trim());
        task.setDescription(request.getDescription());
        task.setSkillTags(request.getSkillTags() == null ? new ArrayList<>() : new ArrayList<>(request.getSkillTags()));
        task.setPriority(validatedOrDefault(request.getPriority(), TaskBoardValues.PRIORITIES,
                TaskBoardValues.DEFAULT_PRIORITY, "priority"));
        task.setAssignmentType(validatedOrDefault(request.getAssignmentType(), TaskBoardValues.ASSIGNMENT_TYPES,
                TaskBoardValues.DEFAULT_ASSIGNMENT_TYPE, "assignmentType"));
        task.setStatus(TaskBoardValues.STATUS_BACKLOG);
        task.setSpecId(request.getSpecId());
        if (task.getTenantId() == null) {
            task.setTenantId(TenantContextHolder.getTenantId());
        }
        if (task.getTenantId() == null || task.getTenantId().isBlank()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "tenant context is required to create a task");
        }
        boolean saved = save(task);
        if (!saved) {
            throw new BaseException(ResultCode.INTERNAL_ERROR, "failed to persist task");
        }
        log.info("[TaskBoard] Task created: id={}, tenantId={}, title={}", task.getId(), task.getTenantId(), task.getTitle());
        return getById(task.getId());
    }

    @Override
    public SfTask updateTask(Long id, TaskUpdateRequest request) {
        SfTask task = requireTask(id);
        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            task.setTitle(request.getTitle().trim());
        }
        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
        }
        if (request.getSkillTags() != null) {
            task.setSkillTags(new ArrayList<>(request.getSkillTags()));
        }
        if (request.getPriority() != null) {
            task.setPriority(validated(request.getPriority(), TaskBoardValues.PRIORITIES, "priority"));
        }
        if (request.getAssignmentType() != null) {
            task.setAssignmentType(validated(request.getAssignmentType(), TaskBoardValues.ASSIGNMENT_TYPES, "assignmentType"));
        }
        if (request.getSpecId() != null) {
            task.setSpecId(request.getSpecId());
        }
        task.setUpdatedAt(LocalDateTime.now());
        boolean updated = updateById(task);
        if (!updated) {
            throw new BaseException(ResultCode.INTERNAL_ERROR, "failed to update task " + id);
        }
        return getById(id);
    }

    @Override
    public void updateStatus(Long id, TaskStatusUpdateRequest request) {
        SfTask task = requireTask(id);
        String status = request.getStatus().trim();
        if (!TaskBoardValues.STATUSES.contains(status)) {
            throw new BaseException(ResultCode.PARAM_ERROR, "unknown task status: " + status);
        }
        if (TaskBoardValues.STATUS_BLOCKED.equals(status)
                && !StringUtils.hasText(request.getBlockerReason())) {
            throw new BaseException(ResultCode.PARAM_ERROR, "blockerReason is required when status is BLOCKED");
        }
        task.setStatus(status);
        task.setBlockerReason(TaskBoardValues.STATUS_BLOCKED.equals(status) ? request.getBlockerReason().trim() : null);
        task.setUpdatedAt(LocalDateTime.now());
        boolean updated = updateById(task);
        if (!updated) {
            throw new BaseException(ResultCode.INTERNAL_ERROR, "failed to update status of task " + id);
        }
        log.info("[TaskBoard] Task status changed: id={}, status={}", id, status);
    }

    @Override
    public void deleteTask(Long id) {
        requireTask(id);
        boolean removed = removeById(id);
        if (!removed) {
            throw new BaseException(ResultCode.INTERNAL_ERROR, "failed to delete task " + id);
        }
        log.info("[TaskBoard] Task deleted: id={}", id);
    }

    private SfTask requireTask(Long id) {
        SfTask task = getById(id);
        if (task == null) {
            throw new BaseException(ResultCode.NOT_FOUND, "task not found: " + id);
        }
        return task;
    }

    private String validatedOrDefault(String value, java.util.Set<String> allowed, String fallback, String field) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return validated(value, allowed, field);
    }

    private String validated(String value, java.util.Set<String> allowed, String field) {
        if (!allowed.contains(value)) {
            throw new BaseException(ResultCode.PARAM_ERROR, "unknown " + field + ": " + value);
        }
        return value;
    }
}
