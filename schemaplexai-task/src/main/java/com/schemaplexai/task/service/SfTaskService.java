package com.schemaplexai.task.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.schemaplexai.task.dto.TaskCreateRequest;
import com.schemaplexai.task.dto.TaskStatusUpdateRequest;
import com.schemaplexai.task.dto.TaskUpdateRequest;
import com.schemaplexai.task.entity.SfTask;
import com.schemaplexai.task.vo.TaskPageResult;

/**
 * Task board item service ({@code /task/tasks} REST layer).
 */
public interface SfTaskService extends IService<SfTask> {

    /**
     * Paged, filtered board listing.
     *
     * @param page     1-based page number (clamped to &gt;= 1)
     * @param pageSize page size (clamped to 1..1000; the frontend board requests up to 1000)
     * @param status   optional exact status filter
     * @param priority optional exact priority filter
     * @param keyword  optional keyword matched against title and description
     */
    TaskPageResult listTasks(int page, int pageSize, String status, String priority, String keyword);

    SfTask createTask(TaskCreateRequest request);

    SfTask updateTask(Long id, TaskUpdateRequest request);

    void updateStatus(Long id, TaskStatusUpdateRequest request);

    void deleteTask(Long id);
}
