package com.schemaplexai.agent.engine.tool;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ToolCallBudgetServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    private ToolCallBudgetService service;

    @BeforeEach
    void setUp() {
        service = new ToolCallBudgetService(redisTemplate);
        ReflectionTestUtils.setField(service, "dailyLimit", 500L);
    }

    // ------------------------------------------------------------------
    // hasRemainingBudget
    // ------------------------------------------------------------------

    @Test
    void hasRemainingBudget_underLimit_returnsTrue() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn("100");

        assertTrue(service.hasRemainingBudget("tenant-1"));
    }

    @Test
    void hasRemainingBudget_atLimit_returnsFalse() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn("500");

        assertFalse(service.hasRemainingBudget("tenant-1"));
    }

    @Test
    void hasRemainingBudget_overLimit_returnsFalse() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn("600");

        assertFalse(service.hasRemainingBudget("tenant-1"));
    }

    @Test
    void hasRemainingBudget_noEntry_returnsTrue() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn(null);

        assertTrue(service.hasRemainingBudget("tenant-1"));
    }

    // ------------------------------------------------------------------
    // consume
    // ------------------------------------------------------------------

    @Test
    void consume_firstCall_setsTtlAndReturnsTrue() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(anyString())).thenReturn(1L);

        assertTrue(service.consume("tenant-1"));

        verify(redisTemplate).expire(anyString(), eq(Duration.ofHours(24)));
    }

    @Test
    void consume_withinBudget_returnsTrue() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(anyString())).thenReturn(250L);

        assertTrue(service.consume("tenant-1"));

        verify(redisTemplate, never()).expire(anyString(), any());
    }

    @Test
    void consume_exceedsBudget_returnsFalse() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.increment(anyString())).thenReturn(501L);

        assertFalse(service.consume("tenant-1"));
    }

    // ------------------------------------------------------------------
    // getCurrentCount
    // ------------------------------------------------------------------

    @Test
    void getCurrentCount_noEntry_returnsZero() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn(null);

        assertEquals(0, service.getCurrentCount("tenant-1"));
    }

    @Test
    void getCurrentCount_hasEntry_returnsCount() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn("42");

        assertEquals(42, service.getCurrentCount("tenant-1"));
    }

    // ------------------------------------------------------------------
    // getRemaining
    // ------------------------------------------------------------------

    @Test
    void getRemaining_underLimit_returnsRemaining() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn("200");

        assertEquals(300, service.getRemaining("tenant-1"));
    }

    @Test
    void getRemaining_overLimit_returnsZero() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn("600");

        assertEquals(0, service.getRemaining("tenant-1"));
    }

    @Test
    void getRemaining_noEntry_returnsFullLimit() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn(null);

        assertEquals(500, service.getRemaining("tenant-1"));
    }

    // ------------------------------------------------------------------
    // getDailyLimit
    // ------------------------------------------------------------------

    @Test
    void getDailyLimit_returnsConfiguredValue() {
        assertEquals(500, service.getDailyLimit());
    }
}
