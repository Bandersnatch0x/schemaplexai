package com.schemaplexai.task.mq;

import com.schemaplexai.common.constants.CommonConstants;
import com.schemaplexai.task.entity.SfIdempotencyKey;
import com.schemaplexai.task.mapper.SfIdempotencyKeyMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.amqp.core.Message;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class MqIdempotencyInterceptor {

    private final StringRedisTemplate redisTemplate;
    private final SfIdempotencyKeyMapper idempotencyKeyMapper;

    @Around("@annotation(org.springframework.amqp.rabbit.annotation.RabbitListener)")
    @Transactional(rollbackFor = Exception.class)
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        Message message = null;
        for (Object arg : args) {
            if (arg instanceof Message) {
                message = (Message) arg;
                break;
            }
        }

        if (message == null) {
            return joinPoint.proceed();
        }

        String messageId = message.getMessageProperties().getMessageId();
        if (messageId == null || messageId.isBlank()) {
            messageId = new String(message.getBody(), StandardCharsets.UTF_8);
        }

        String consumerGroup = joinPoint.getTarget().getClass().getSimpleName();
        String redisKey = String.format(CommonConstants.REDIS_KEY_IDEMPOTENCY, consumerGroup + ":" + messageId);

        // 1. Check Redis
        Boolean existsInRedis = redisTemplate.hasKey(redisKey);
        if (Boolean.TRUE.equals(existsInRedis)) {
            log.warn("[MQ Idempotency] Message already consumed, skip. messageId={}, consumerGroup={}", messageId, consumerGroup);
            return null;
        }

        // 2. Try to insert DB record as distributed lock
        SfIdempotencyKey record = new SfIdempotencyKey();
        record.setMessageId(messageId);
        record.setConsumerGroup(consumerGroup);
        record.setStatus("PROCESSING");
        record.setConsumedAt(LocalDateTime.now());
        try {
            idempotencyKeyMapper.insert(record);
        } catch (DuplicateKeyException e) {
            log.warn("[MQ Idempotency] Message already consumed in DB, skip. messageId={}, consumerGroup={}", messageId, consumerGroup);
            redisTemplate.opsForValue().set(redisKey, "1", 24, TimeUnit.HOURS);
            return null;
        }

        // 3. Execute business logic
        Object result;
        try {
            result = joinPoint.proceed();
            record.setStatus("SUCCESS");
        } catch (Exception e) {
            record.setStatus("FAILED");
            idempotencyKeyMapper.updateById(record);
            throw e;
        }

        // 4. Update PostgreSQL
        idempotencyKeyMapper.updateById(record);

        // 5. Write Redis
        redisTemplate.opsForValue().set(redisKey, "1", 24, TimeUnit.HOURS);

        log.info("[MQ Idempotency] Message consumed successfully. messageId={}, consumerGroup={}", messageId, consumerGroup);
        return result;
    }
}
