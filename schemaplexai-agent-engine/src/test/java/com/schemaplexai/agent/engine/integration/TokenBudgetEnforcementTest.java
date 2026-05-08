package com.schemaplexai.agent.engine.integration;

import com.schemaplexai.agent.engine.admission.*;
import com.schemaplexai.agent.engine.tool.ToolCallBudgetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Token Budget Enforcement Integration")
class TokenBudgetEnforcementTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ToolCallBudgetService toolCallBudgetService;

    @InjectMocks
    private ExecutionAdmissionService admissionService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(toolCallBudgetService.hasRemainingBudget(anyString())).thenReturn(true);
    }

    @Test
    @DisplayName("should allow execution within budget")
    void shouldAllowWithinBudget() {
        TokenBudget budget = new TokenBudget(1000, 500);

        AdmissionResult result = admissionService.admit("tenant-1", 1L, budget);

        assertTrue(result.isAllowed());
        assertEquals("OK", result.getReason());
    }

    @Test
    @DisplayName("should reject when token budget is exceeded")
    void shouldRejectWhenBudgetExceeded() {
        TokenBudget budget = new TokenBudget(100, 50);
        budget.getConsumedInputTokens().set(101);

        AdmissionResult result = admissionService.admit("tenant-1", 1L, budget);

        assertFalse(result.isAllowed());
        assertEquals("Token budget exceeded", result.getReason());
        assertEquals(CompressionStrategy.SUMMARIZE, result.getSuggestedCompression());
    }

    @Test
    @DisplayName("should reject when output budget is exceeded")
    void shouldRejectWhenOutputBudgetExceeded() {
        TokenBudget budget = new TokenBudget(1000, 10);
        budget.getConsumedOutputTokens().set(11);

        AdmissionResult result = admissionService.admit("tenant-1", 1L, budget);

        assertFalse(result.isAllowed());
        assertEquals("Token budget exceeded", result.getReason());
        assertEquals(CompressionStrategy.SUMMARIZE, result.getSuggestedCompression());
    }

    @Test
    @DisplayName("should track concurrent token consumption accurately")
    void shouldTrackConsumptionAccurately() {
        TokenBudget budget = new TokenBudget(1000, 500);
        budget.consumeInput(300);
        budget.consumeOutput(200);

        assertEquals(700, budget.remainingInput());
        assertEquals(300, budget.remainingOutput());
        assertFalse(budget.isExceeded());
        assertTrue(budget.hasRemaining());
    }

    @Test
    @DisplayName("should suggest compression strategy when budget exceeded")
    void shouldSuggestCompressionWhenExceeded() {
        TokenBudget budget = new TokenBudget(100, 50);
        budget.getConsumedInputTokens().set(101);

        AdmissionResult result = admissionService.admit("tenant-1", 1L, budget);

        assertFalse(result.isAllowed());
        assertNotNull(result.getSuggestedCompression());
        assertEquals(CompressionStrategy.SUMMARIZE, result.getSuggestedCompression());
    }

    @Test
    @DisplayName("should reject when rate limit exceeded")
    void shouldRejectWhenRateLimitExceeded() {
        when(valueOperations.increment(anyString())).thenReturn(61L);

        TokenBudget budget = new TokenBudget(1000, 500);
        AdmissionResult result = admissionService.admit("tenant-1", 1L, budget);

        assertFalse(result.isAllowed());
        assertEquals("Rate limit exceeded", result.getReason());
    }

    @Test
    @DisplayName("should reject when concurrency limit exceeded")
    void shouldRejectWhenConcurrencyExceeded() {
        when(valueOperations.increment(anyString())).thenReturn(1L, 6L);

        TokenBudget budget = new TokenBudget(1000, 500);
        AdmissionResult result = admissionService.admit("tenant-1", 1L, budget);

        assertFalse(result.isAllowed());
        assertEquals("Concurrency limit exceeded", result.getReason());
    }

    @Test
    @DisplayName("should reject when daily cost budget exceeded")
    void shouldRejectWhenCostBudgetExceeded() {
        when(valueOperations.get(anyString())).thenReturn("150.0");

        TokenBudget budget = new TokenBudget(1000, 500);
        AdmissionResult result = admissionService.admit("tenant-1", 1L, budget);

        assertFalse(result.isAllowed());
        assertEquals("Daily cost budget exceeded", result.getReason());
    }
}
