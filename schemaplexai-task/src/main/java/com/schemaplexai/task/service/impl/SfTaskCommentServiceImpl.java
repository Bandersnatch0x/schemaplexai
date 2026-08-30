package com.schemaplexai.task.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.schemaplexai.common.context.TenantContextHolder;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.task.entity.SfTask;
import com.schemaplexai.task.entity.SfTaskComment;
import com.schemaplexai.task.mapper.SfTaskCommentMapper;
import com.schemaplexai.task.mapper.SfTaskMapper;
import com.schemaplexai.task.mapper.SfUserLookupMapper;
import com.schemaplexai.task.service.SfTaskCommentService;
import com.schemaplexai.task.vo.TaskCommentVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Task comment service implementation. Tenant isolation is enforced by the
 * {@code TenantLineInnerInterceptor}; the {@code sf_user} lookup used to resolve
 * the author display name scopes its tenant explicitly because that table is on
 * the interceptor's ignore list.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SfTaskCommentServiceImpl extends ServiceImpl<SfTaskCommentMapper, SfTaskComment>
        implements SfTaskCommentService {

    private final SfTaskMapper taskMapper;
    private final SfUserLookupMapper userLookupMapper;

    @Override
    public List<TaskCommentVO> listComments(Long taskId) {
        requireTask(taskId);
        List<SfTaskComment> comments = lambdaQuery()
                .eq(SfTaskComment::getTaskId, taskId)
                .orderByAsc(SfTaskComment::getCreatedAt)
                .list();
        List<TaskCommentVO> result = new ArrayList<>(comments.size());
        for (SfTaskComment comment : comments) {
            result.add(TaskCommentVO.from(comment));
        }
        return result;
    }

    @Override
    public TaskCommentVO addComment(Long taskId, String content, Long authorId) {
        requireTask(taskId);
        if (!StringUtils.hasText(content)) {
            throw new BaseException(ResultCode.PARAM_ERROR, "comment content is required");
        }
        if (authorId == null) {
            throw new BaseException(ResultCode.UNAUTHORIZED, "missing user identity for comment author");
        }

        SfTaskComment comment = new SfTaskComment();
        comment.setTaskId(taskId);
        comment.setContent(content.trim());
        comment.setAuthorId(authorId);
        comment.setAuthorName(resolveAuthorName(authorId));
        if (comment.getTenantId() == null) {
            comment.setTenantId(TenantContextHolder.getTenantId());
        }
        boolean saved = save(comment);
        if (!saved) {
            throw new BaseException(ResultCode.INTERNAL_ERROR, "failed to persist task comment");
        }
        log.info("[TaskBoard] Comment added: taskId={}, authorId={}", taskId, authorId);
        return TaskCommentVO.from(getById(comment.getId()));
    }

    private void requireTask(Long taskId) {
        SfTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BaseException(ResultCode.NOT_FOUND, "task not found: " + taskId);
        }
    }

    /**
     * Best-effort display-name resolution: a missing/unresolvable author keeps
     * the comment valid (the frontend marks {@code authorName} optional).
     */
    private String resolveAuthorName(Long authorId) {
        String tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            return null;
        }
        try {
            return userLookupMapper.findUsernameById(authorId, Long.valueOf(tenantId));
        } catch (NumberFormatException e) {
            log.debug("[TaskBoard] Non-numeric tenant id, skipping author name lookup: {}", tenantId);
            return null;
        }
    }
}
