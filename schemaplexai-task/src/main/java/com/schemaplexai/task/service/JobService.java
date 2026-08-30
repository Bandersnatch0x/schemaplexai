package com.schemaplexai.task.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.task.entity.SfMessageFailLog;
import com.schemaplexai.task.mapper.SfMessageFailLogMapper;
import com.schemaplexai.task.vo.JobPageResult;
import com.schemaplexai.task.vo.JobRecordVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Async job board service ({@code /task/jobs} REST layer).
 *
 * <p>Jobs are projected from the existing {@code sf_message_fail_log} table —
 * every message that exhausted its consumer retries lands there via
 * {@code MessageFailLogService}. Retry delegates to the existing
 * {@link DeadLetterRetryService} (which republishes the stored payload and marks
 * the record {@code RETRIED}); records whose AMQP message id is not a UUID
 * cannot be replayed through that service and are instead marked
 * {@code RETRY_PENDING} for manual follow-up.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobService {

    static final String STATUS_PENDING = "PENDING";
    static final String STATUS_RETRIED = "RETRIED";
    static final String STATUS_CANCELLED = "CANCELLED";
    static final String STATUS_RETRY_PENDING = "RETRY_PENDING";

    private static final int MAX_PAGE_SIZE = 1000;

    private final SfMessageFailLogMapper messageFailLogMapper;
    private final DeadLetterRetryService deadLetterRetryService;

    public JobPageResult listJobs(int page, int pageSize, String queue) {
        long current = Math.max(page, 1);
        long size = Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);

        Page<SfMessageFailLog> pageResult = messageFailLogMapper.selectPage(
                new Page<>(current, size),
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SfMessageFailLog>()
                        .and(queue != null && !queue.isBlank(), w -> w
                                .eq(SfMessageFailLog::getRoutingKey, queue)
                                .or(o -> o.isNull(SfMessageFailLog::getRoutingKey)
                                        .eq(SfMessageFailLog::getConsumerGroup, queue)))
                        .orderByDesc(SfMessageFailLog::getCreatedAt));

        List<JobRecordVO> list = new ArrayList<>(pageResult.getRecords().size());
        for (SfMessageFailLog failLog : pageResult.getRecords()) {
            list.add(JobRecordVO.from(failLog));
        }
        return new JobPageResult(list, pageResult.getTotal());
    }

    /**
     * Retries a pending job. When the stored AMQP message id is a UUID the
     * existing {@link DeadLetterRetryService} republishes the original payload;
     * otherwise the record is only marked {@code RETRY_PENDING} (note: no
     * automatic republish is possible without the UUID-based replay path).
     */
    public void retryJob(Long id) {
        SfMessageFailLog failLog = requireJob(id);
        if (!STATUS_PENDING.equals(failLog.getStatus())) {
            throw new BaseException(ResultCode.PARAM_ERROR,
                    "job " + id + " is not retryable in status " + failLog.getStatus());
        }

        UUID eventId = parseEventId(failLog.getMessageId());
        if (eventId != null) {
            deadLetterRetryService.retryDeadEvent(eventId);
            log.info("[JobBoard] Job retried via dead letter replay: id={}, messageId={}", id, eventId);
            return;
        }

        // Fallback: the fail log carries no UUID message id, so the dead letter
        // replay cannot target it. Mark it awaiting republish instead of failing
        // silently (ticket: "otherwise mark the record as pending republish").
        failLog.setStatus(STATUS_RETRY_PENDING);
        failLog.setRetryCount(failLog.getRetryCount() == null ? 1 : failLog.getRetryCount() + 1);
        failLog.setUpdatedAt(LocalDateTime.now());
        int updated = messageFailLogMapper.updateById(failLog);
        if (updated <= 0) {
            throw new BaseException(ResultCode.INTERNAL_ERROR, "failed to mark job " + id + " as retry pending");
        }
        log.warn("[JobBoard] Job {} has no UUID message id; marked RETRY_PENDING (manual republish required)", id);
    }

    /** Cancels a job that has not been republished yet. */
    public void cancelJob(Long id) {
        SfMessageFailLog failLog = requireJob(id);
        if (STATUS_CANCELLED.equals(failLog.getStatus())) {
            return;
        }
        if (STATUS_RETRIED.equals(failLog.getStatus())) {
            throw new BaseException(ResultCode.PARAM_ERROR,
                    "job " + id + " was already republished and cannot be cancelled");
        }
        failLog.setStatus(STATUS_CANCELLED);
        failLog.setUpdatedAt(LocalDateTime.now());
        int updated = messageFailLogMapper.updateById(failLog);
        if (updated <= 0) {
            throw new BaseException(ResultCode.INTERNAL_ERROR, "failed to cancel job " + id);
        }
        log.info("[JobBoard] Job cancelled: id={}", id);
    }

    private SfMessageFailLog requireJob(Long id) {
        SfMessageFailLog failLog = messageFailLogMapper.selectById(id);
        if (failLog == null) {
            throw new BaseException(ResultCode.NOT_FOUND, "job not found: " + id);
        }
        return failLog;
    }

    private UUID parseEventId(String messageId) {
        if (messageId == null || messageId.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(messageId.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
