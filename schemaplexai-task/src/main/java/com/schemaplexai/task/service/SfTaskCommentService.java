package com.schemaplexai.task.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.schemaplexai.task.entity.SfTaskComment;
import com.schemaplexai.task.vo.TaskCommentVO;

import java.util.List;

/**
 * Task comment service ({@code /task/tasks/{taskId}/comments}).
 */
public interface SfTaskCommentService extends IService<SfTaskComment> {

    /** Comments of an existing task, oldest first. */
    List<TaskCommentVO> listComments(Long taskId);

    /** Adds a comment authored by {@code authorId} to an existing task. */
    TaskCommentVO addComment(Long taskId, String content, Long authorId);
}
