package com.schemaplexai.task.scheduling;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemoryConsolidationJobTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private MemoryConsolidationJob job;

    @Test
    void run_withKeys_refreshesTtl() {
        Set<String> keys = new HashSet<>();
        keys.add("chat:memory:user1");
        keys.add("chat:memory:user2");

        when(redisTemplate.keys("chat:memory:*")).thenReturn(keys);

        assertThatNoException().isThrownBy(() -> job.run());

        verify(redisTemplate, times(2)).expire(anyString(), any());
    }

    @Test
    void run_noKeys_doesNotThrow() {
        when(redisTemplate.keys("chat:memory:*")).thenReturn(null);

        assertThatNoException().isThrownBy(() -> job.run());

        verify(redisTemplate, never()).expire(anyString(), any());
    }

    @Test
    void run_emptyKeys_doesNotThrow() {
        when(redisTemplate.keys("chat:memory:*")).thenReturn(new HashSet<>());

        assertThatNoException().isThrownBy(() -> job.run());

        verify(redisTemplate, never()).expire(anyString(), any());
    }

    @Test
    void run_redisKeysThrowsException_propagates() {
        when(redisTemplate.keys("chat:memory:*")).thenThrow(new RuntimeException("Redis error"));

        assertThatThrownBy(() -> job.run())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Redis error");
    }

    @Test
    void run_expireThrowsException_propagates() {
        Set<String> keys = new HashSet<>();
        keys.add("chat:memory:user1");
        when(redisTemplate.keys("chat:memory:*")).thenReturn(keys);
        when(redisTemplate.expire(anyString(), any())).thenThrow(new RuntimeException("expire failed"));

        assertThatThrownBy(() -> job.run())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("expire failed");
    }
}
