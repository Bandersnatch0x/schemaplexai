package com.schemaplexai.task.vo;

import com.schemaplexai.task.domain.TaskBoardValues;
import com.schemaplexai.task.entity.SfMessageFailLog;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * View of an async job, matching {@code JobRecord} in
 * {@code schemaplexai-ui/src/api/task.ts}. Jobs are projected from the existing
 * {@code sf_message_fail_log} table:
 * <ul>
 *   <li>{@code name} — the AMQP message id (falls back to the fail-log row id)</li>
 *   <li>{@code queue} — the routing key the message was delivered on
 *       (falls back to the consumer group)</li>
 *   <li>{@code maxRetries} — no such column exists on the fail-log table, so the
 *       platform default is reported</li>
 * </ul>
 */
@Data
public class JobRecordVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private String queue;
    private String status;
    private int retryCount;
    private int maxRetries;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static JobRecordVO from(SfMessageFailLog failLog) {
        JobRecordVO vo = new JobRecordVO();
        vo.setId(String.valueOf(failLog.getId()));
        vo.setName(failLog.getMessageId() != null ? failLog.getMessageId() : "fail-log-" + failLog.getId());
        vo.setQueue(failLog.getRoutingKey() != null ? failLog.getRoutingKey() : failLog.getConsumerGroup());
        vo.setStatus(failLog.getStatus());
        vo.setRetryCount(failLog.getRetryCount() == null ? 0 : failLog.getRetryCount());
        vo.setMaxRetries(TaskBoardValues.JOB_DEFAULT_MAX_RETRIES);
        vo.setCreatedAt(failLog.getCreatedAt());
        vo.setUpdatedAt(failLog.getUpdatedAt());
        return vo;
    }
}
