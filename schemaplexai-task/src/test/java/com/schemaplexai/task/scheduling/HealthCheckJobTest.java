package com.schemaplexai.task.scheduling;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HealthCheckJobTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private HealthCheckJob job;

    @Test
    void run_redisOk_doesNotThrow() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("health:check")).thenReturn("ok");

        assertThatNoException().isThrownBy(() -> job.run());
    }

    @Test
    void run_redisThrowsException_doesNotThrow() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("health:check")).thenThrow(new RuntimeException("Redis down"));

        assertThatNoException().isThrownBy(() -> job.run());
    }
}
