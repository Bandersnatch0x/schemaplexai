package com.schemaplexai.agent.engine.memory;

import com.schemaplexai.agent.engine.model.LlmMessage;
import com.schemaplexai.common.context.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompositeChatMemoryStoreCompactionTest {

    @Mock
    private RedisTemplate<String, LlmMessage> redisTemplate;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ListOperations<String, LlmMessage> listOps;

    @Mock
    private ValueOperations<String, String> stringOps;

    private CompositeChatMemoryStore store;

    @BeforeEach
    void setUp() {
        TenantKeyService keyService = new TenantKeyService("test-master-secret-for-unit-tests-32bytes!");
        store = new CompositeChatMemoryStore(redisTemplate, stringRedisTemplate, null, keyService);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void shouldEstimateTokensForMessages() {
        List<LlmMessage> messages = List.of(
            new LlmMessage("user", "Hello world"),
            new LlmMessage("assistant", "How can I help?")
        );

        int tokens = store.estimateTokens(messages);

        assertThat(tokens).isPositive();
        // user (4) + Hello world (11) + overhead (4) = 19 chars => ~4-5 tokens
        // assistant (9) + How can I help? (16) + overhead (4) = 29 chars => ~7 tokens
        // Total ~11-12 tokens
        assertThat(tokens).isBetween(10, 15);
    }

    @Test
    void shouldReturnZeroForEmptyMessages() {
        assertThat(store.estimateTokens(List.of())).isZero();
        assertThat(store.estimateTokens(null)).isZero();
    }

    @Test
    void shouldReplaceMessages() {
        TenantContextHolder.setTenantId("tenant-1");
        when(redisTemplate.delete(anyString())).thenReturn(true);
        when(redisTemplate.opsForList()).thenReturn(listOps);
        when(listOps.rightPushAll(anyString(), any(LlmMessage[].class))).thenReturn(1L);

        List<LlmMessage> newMessages = List.of(
            new LlmMessage("system", "Summary of conversation"),
            new LlmMessage("user", "Follow-up question")
        );

        store.replaceMessages("conv-1", newMessages);

        verify(redisTemplate).delete(anyString());
        verify(listOps).rightPushAll(anyString(), any(LlmMessage[].class));
    }

    @Test
    void shouldHandleNullOrBlankConversationId() {
        store.replaceMessages(null, List.of(new LlmMessage("user", "test")));
        store.replaceMessages("   ", List.of(new LlmMessage("user", "test")));
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void shouldHandleNullMessages() {
        TenantContextHolder.setTenantId("tenant-1");
        when(redisTemplate.delete(anyString())).thenReturn(true);

        store.replaceMessages("conv-1", null);

        verify(redisTemplate).delete(anyString());
        verify(listOps, never()).rightPushAll(anyString(), any(LlmMessage[].class));
    }
}
