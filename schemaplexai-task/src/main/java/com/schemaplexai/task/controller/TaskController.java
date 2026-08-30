package com.schemaplexai.task.controller;

import com.schemaplexai.common.controller.BaseController;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.task.dto.CommentCreateRequest;
import com.schemaplexai.task.dto.TaskCreateRequest;
import com.schemaplexai.task.dto.TaskStatusUpdateRequest;
import com.schemaplexai.task.dto.TaskUpdateRequest;
import com.schemaplexai.task.service.SfTaskCommentService;
import com.schemaplexai.task.service.SfTaskService;
import com.schemaplexai.task.vo.TaskCommentVO;
import com.schemaplexai.task.vo.TaskPageResult;
import com.schemaplexai.task.vo.TaskVO;
import com.schemaplexai.task.web.TaskUserContextHolder;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Task board REST endpoints consumed by the frontend task board
 * ({@code schemaplexai-ui/src/api/task.ts}). All responses use the uniform
 * {@link Result} envelope; the list endpoint returns the frontend-contract
 * {@code {list, total}} shape.
 */
@RestController
@RequestMapping("/task/tasks")
@RequiredArgsConstructor
public class TaskController extends BaseController {

    private final SfTaskService taskService;
    private final SfTaskCommentService taskCommentService;

    @GetMapping
    public Result<TaskPageResult> listTasks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String keyword) {
        return success(taskService.listTasks(page, pageSize, status, priority, keyword));
    }

    @GetMapping("/{id}")
    public Result<TaskVO> getTask(@PathVariable String id) {
        var task = taskService.getById(parseId(id));
        if (task == null) {
            throw new BaseException(ResultCode.NOT_FOUND, "task not found: " + id);
        }
        return success(TaskVO.from(task));
    }

    @PostMapping
    public Result<TaskVO> createTask(@Valid @RequestBody TaskCreateRequest request) {
        return success(TaskVO.from(taskService.createTask(request)));
    }

    @PutMapping("/{id}")
    public Result<TaskVO> updateTask(@PathVariable String id, @Valid @RequestBody TaskUpdateRequest request) {
        return success(TaskVO.from(taskService.updateTask(parseId(id), request)));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteTask(@PathVariable String id) {
        taskService.deleteTask(parseId(id));
        return success();
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateTaskStatus(@PathVariable String id, @Valid @RequestBody TaskStatusUpdateRequest request) {
        taskService.updateStatus(parseId(id), request);
        return success();
    }

    @GetMapping("/{taskId}/comments")
    public Result<List<TaskCommentVO>> listComments(@PathVariable String taskId) {
        return success(taskCommentService.listComments(parseId(taskId)));
    }

    @PostMapping("/{taskId}/comments")
    public Result<TaskCommentVO> addComment(@PathVariable String taskId, @Valid @RequestBody CommentCreateRequest request) {
        Long authorId = TaskUserContextHolder.getUserId();
        return success(taskCommentService.addComment(parseId(taskId), request.getContent(), authorId));
    }

    private Long parseId(String id) {
        try {
            return Long.valueOf(id);
        } catch (NumberFormatException e) {
            throw new BaseException(ResultCode.PARAM_ERROR, "invalid id: " + id);
        }
    }
}
