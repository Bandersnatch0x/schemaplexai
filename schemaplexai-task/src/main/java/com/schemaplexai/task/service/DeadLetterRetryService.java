package com.schemaplexai.task.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.task.entity.SfMessageFailLog;
import com.schemaplexai.task.mapper.SfMessageFailLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

/**
 * Service for manually retrying dead letter events.
 *
 * <p>Uses {@code sf_message_fail_log} as the retry source. The supplied UUID is
 * matched against the original AMQP {@code messageId} stored by
 * {@link com.schemaplexai.task.mq.MessageFailLogService}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeadLetterRetryService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_RETRIED = "RETRIED";
    private static final int MAX_LIST_LIMIT = 100;

    private final SfMessageFailLogMapper messageFailLogMapper;
    private final RabbitTemplate rabbitTemplate;

    /**
     * Finds the pending failed message for the given message id and republishes it.
     *
     * @param eventId the UUID stored as the failed message id
     */
    public void retryDeadEvent(UUID eventId) {
        if (eventId == null) {
            throw new BaseException(ResultCode.PARAM_ERROR, "eventId must not be null");
        }
        log.info("[DeadLetterRetryService] Manual retry requested for dead event: eventId={}", eventId);

        SfMessageFailLog failLog = messageFailLogMapper.selectOne(
                new LambdaQueryWrapper<SfMessageFailLog>()
                        .eq(SfMessageFailLog::getMessageId, eventId.toString())
                        .eq(SfMessageFailLog::getStatus, STATUS_PENDING)
                        .last("LIMIT 1"));

        if (failLog == null) {
            throw new BaseException(ResultCode.NOT_FOUND,
                    "pending dead letter message not found for eventId=" + eventId);
        }
        validateRetryTarget(failLog);

        rabbitTemplate.convertAndSend(failLog.getExchange(), failLog.getRoutingKey(), failLog.getPayload());

        failLog.setRetryCount(failLog.getRetryCount() == null ? 1 : failLog.getRetryCount() + 1);
        failLog.setStatus(STATUS_RETRIED);
        int updated = messageFailLogMapper.updateById(failLog);
        if (updated <= 0) {
            throw new BaseException(ResultCode.INTERNAL_ERROR,
                    "failed to mark dead letter message retried for eventId=" + eventId);
        }
    }

    /**
     * Lists recent dead events.
     *
     * @param limit maximum number of events to return
     * @return recent pending failed messages
     */
    public List<SfMessageFailLog> listDeadEvents(int limit) {
        if (limit <= 0) {
            throw new BaseException(ResultCode.PARAM_ERROR, "limit must be greater than 0");
        }
        int boundedLimit = Math.min(limit, MAX_LIST_LIMIT);
        log.debug("[DeadLetterRetryService] listDeadEvents called with limit={}", limit);
        return messageFailLogMapper.selectList(
                new LambdaQueryWrapper<SfMessageFailLog>()
                        .eq(SfMessageFailLog::getStatus, STATUS_PENDING)
                        .orderByDesc(SfMessageFailLog::getCreatedAt)
                        .last("LIMIT " + boundedLimit));
    }

    private void validateRetryTarget(SfMessageFailLog failLog) {
        if (!StringUtils.hasText(failLog.getExchange())) {
            throw new BaseException(ResultCode.INTERNAL_ERROR,
                    "dead letter message exchange is missing for messageId=" + failLog.getMessageId());
        }
        if (!StringUtils.hasText(failLog.getRoutingKey())) {
            throw new BaseException(ResultCode.INTERNAL_ERROR,
                    "dead letter message routing key is missing for messageId=" + failLog.getMessageId());
        }
        if (failLog.getPayload() == null) {
            throw new BaseException(ResultCode.INTERNAL_ERROR,
                    "dead letter message payload is missing for messageId=" + failLog.getMessageId());
        }
    }
}
