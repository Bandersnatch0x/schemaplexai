package com.schemaplexai.task.mq;

import com.schemaplexai.common.constants.CommonConstants;
import com.schemaplexai.task.entity.SfIdempotencyKey;
import com.schemaplexai.task.mapper.SfIdempotencyKeyMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MqIdempotencyInterceptorTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private SfIdempotencyKeyMapper idempotencyKeyMapper;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private MqIdempotencyInterceptor interceptor;

    private Message createMessage(String messageId) {
        MessageProperties properties = new MessageProperties();
        properties.setMessageId(messageId);
        return new Message("body".getBytes(StandardCharsets.UTF_8), properties);
    }

    @Test
    void around_noMessageArg_proceedsDirectly() throws Throwable {
        when(joinPoint.getArgs()).thenReturn(new Object[]{"not a message"});
        when(joinPoint.proceed()).thenReturn("result");

        Object result = interceptor.around(joinPoint);

        assertThat(result).isEqualTo("result");
        verify(redisTemplate, never()).hasKey(anyString());
    }

    @Test
    void around_messageAlreadyInRedis_returnsNull() throws Throwable {
        Message message = createMessage("msg-001");
        when(joinPoint.getArgs()).thenReturn(new Object[]{message});
        when(joinPoint.getTarget()).thenReturn(new TestConsumer());
        when(redisTemplate.hasKey(anyString())).thenReturn(true);

        Object result = interceptor.around(joinPoint);

        assertThat(result).isNull();
        verify(idempotencyKeyMapper, never()).insert(any());
    }

    @Test
    void around_newMessage_executesAndRecordsSuccess() throws Throwable {
        Message message = createMessage("msg-002");
        when(joinPoint.getArgs()).thenReturn(new Object[]{message});
        when(joinPoint.getTarget()).thenReturn(new TestConsumer());
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        when(joinPoint.proceed()).thenReturn("success");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        Object result = interceptor.around(joinPoint);

        assertThat(result).isEqualTo("success");
        verify(idempotencyKeyMapper).insert(any(SfIdempotencyKey.class));
        verify(idempotencyKeyMapper, times(1)).updateById(any(SfIdempotencyKey.class));
        verify(valueOperations).set(anyString(), eq("1"), eq(24L), eq(TimeUnit.HOURS));
    }

    @Test
    void around_duplicateKeyInDb_returnsNull() throws Throwable {
        Message message = createMessage("msg-003");
        when(joinPoint.getArgs()).thenReturn(new Object[]{message});
        when(joinPoint.getTarget()).thenReturn(new TestConsumer());
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        doThrow(new DuplicateKeyException("dup")).when(idempotencyKeyMapper).insert(any(SfIdempotencyKey.class));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        Object result = interceptor.around(joinPoint);

        assertThat(result).isNull();
        verify(valueOperations).set(anyString(), eq("1"), eq(24L), eq(TimeUnit.HOURS));
        verify(joinPoint, never()).proceed();
    }

    @Test
    void around_businessException_recordsFailedAndRethrows() throws Throwable {
        Message message = createMessage("msg-004");
        when(joinPoint.getArgs()).thenReturn(new Object[]{message});
        when(joinPoint.getTarget()).thenReturn(new TestConsumer());
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        when(joinPoint.proceed()).thenThrow(new RuntimeException("biz error"));

        assertThatThrownBy(() -> interceptor.around(joinPoint))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("biz error");

        verify(idempotencyKeyMapper).insert(any(SfIdempotencyKey.class));
        verify(idempotencyKeyMapper).updateById(argThat(record -> "FAILED".equals(record.getStatus())));
    }

    @Test
    void around_nullMessageId_usesBodyAsFallback() throws Throwable {
        MessageProperties properties = new MessageProperties();
        properties.setMessageId(null);
        Message message = new Message("fallback-body".getBytes(StandardCharsets.UTF_8), properties);

        when(joinPoint.getArgs()).thenReturn(new Object[]{message});
        when(joinPoint.getTarget()).thenReturn(new TestConsumer());
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        when(joinPoint.proceed()).thenReturn("ok");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        interceptor.around(joinPoint);

        verify(idempotencyKeyMapper).insert(argThat(record -> "fallback-body".equals(record.getMessageId())));
    }

    static class TestConsumer {
    }
}
