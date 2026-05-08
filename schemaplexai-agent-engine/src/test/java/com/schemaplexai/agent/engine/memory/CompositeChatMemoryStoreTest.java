package com.schemaplexai.agent.engine.memory;

import com.schemaplexai.agent.engine.admission.TokenBudget;
import com.schemaplexai.agent.engine.entity.SfChatMessage;
import com.schemaplexai.agent.engine.mapper.SfChatMessageMapper;
import com.schemaplexai.agent.engine.model.LlmMessage;
import com.schemaplexai.common.context.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompositeChatMemoryStoreTest {

    @Mock
    private RedisTemplate<String, LlmMessage> redisTemplate;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private SfChatMessageMapper chatMessageMapper;

    @Mock
    private ListOperations<String, LlmMessage> listOps;

    @Mock
    private ValueOperations<String, String> stringOps;

    private TenantKeyService tenantKeyService;
    private CompositeChatMemoryStore memoryStore;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForList()).thenReturn(listOps);
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(stringOps);
        lenient().when(stringOps.setIfAbsent(anyString(), anyString(), any())).thenReturn(true);
        tenantKeyService = new TenantKeyService("test-master-secret-for-unit-tests-32bytes!", true);
        memoryStore = new CompositeChatMemoryStore(redisTemplate, stringRedisTemplate, chatMessageMapper, tenantKeyService);
        TenantContextHolder.setTenantId("test-tenant");
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void loadMessagesReturnsRedisDataWhenAvailable() {
        // Redis stores encrypted content — simulate that
        String encHello = tenantKeyService.encrypt("hello", "test-tenant");
        String encHi = tenantKeyService.encrypt("hi", "test-tenant");
        List<LlmMessage> redisMessages = List.of(
                new LlmMessage("user", encHello),
                new LlmMessage("assistant", encHi)
        );
        when(listOps.range(anyString(), eq(0L), eq(-1L))).thenReturn(redisMessages);
        when(listOps.size(anyString())).thenReturn(2L);

        List<LlmMessage> result = memoryStore.loadMessages("conv-1");

        assertEquals(2, result.size());
        // After decryption, content should be plaintext
        assertEquals("hello", result.get(0).getContent());
        assertEquals("hi", result.get(1).getContent());
        verify(chatMessageMapper, never()).selectList(any());
    }

    @Test
    void loadMessagesFallsBackToDatabaseWhenRedisEmpty() {
        when(listOps.range(anyString(), eq(0L), eq(-1L))).thenReturn(null);
        // DB stores encrypted content — simulate that
        String encHello = tenantKeyService.encrypt("hello", "test-tenant");
        String encHi = tenantKeyService.encrypt("hi", "test-tenant");
        List<SfChatMessage> dbMessages = List.of(
                createDbMessage("conv-1", 0, "user", encHello),
                createDbMessage("conv-1", 1, "assistant", encHi)
        );
        when(chatMessageMapper.selectList(any())).thenReturn(dbMessages);

        List<LlmMessage> result = memoryStore.loadMessages("conv-1");

        assertEquals(2, result.size());
        assertEquals("hello", result.get(0).getContent());
        verify(listOps).rightPushAll(anyString(), any(LlmMessage[].class));
        verify(redisTemplate).expire(anyString(), any());
    }

    @Test
    void loadMessagesReturnsEmptyListWhenBothCachesMiss() {
        when(listOps.range(anyString(), eq(0L), eq(-1L))).thenReturn(null);
        when(chatMessageMapper.selectList(any())).thenReturn(null);

        List<LlmMessage> result = memoryStore.loadMessages("conv-1");

        assertTrue(result.isEmpty());
    }

    @Test
    void saveMessageWritesToDatabaseFirstThenRedis() {
        when(chatMessageMapper.selectOne(any())).thenReturn(null);
        when(listOps.rightPush(anyString(), any(LlmMessage.class))).thenReturn(1L);
        when(listOps.size(anyString())).thenReturn(1L);

        memoryStore.saveMessage("conv-1", new LlmMessage("user", "hello"));

        verify(chatMessageMapper).insert(any(SfChatMessage.class));
        verify(listOps).rightPush(anyString(), any(LlmMessage.class));
    }

    @Test
    void saveMessageTrimsRedisWhenExceedsMax() {
        when(chatMessageMapper.selectOne(any())).thenReturn(null);
        when(listOps.rightPush(anyString(), any(LlmMessage.class))).thenReturn(51L);
        when(listOps.size(anyString())).thenReturn(51L);

        memoryStore.saveMessage("conv-1", new LlmMessage("user", "hello"));

        verify(listOps).trim(anyString(), eq(1L), eq(-1L));
    }

    @Test
    void clearRemovesRedisAndDatabaseEntries() {
        memoryStore.clear("conv-1");

        verify(redisTemplate).delete(anyString());
        verify(chatMessageMapper).delete(any());
    }

    @Test
    void databaseInsertHasCorrectFields() {
        when(chatMessageMapper.selectOne(any())).thenReturn(null);
        when(listOps.rightPush(anyString(), any(LlmMessage.class))).thenReturn(1L);
        when(listOps.size(anyString())).thenReturn(1L);
        ArgumentCaptor<SfChatMessage> captor = ArgumentCaptor.forClass(SfChatMessage.class);

        memoryStore.saveMessage("conv-1", new LlmMessage("assistant", "response"));

        verify(chatMessageMapper).insert(captor.capture());
        SfChatMessage saved = captor.getValue();
        assertEquals("conv-1", saved.getConversationId());
        assertEquals("assistant", saved.getRole());
        // Content is now encrypted — must not be plaintext
        assertNotEquals("response", saved.getContent(), "Content should be encrypted");
    }

    @Test
    void firstSavedMessageGetsTurnIndexZero() {
        when(chatMessageMapper.selectOne(any())).thenReturn(null);
        when(listOps.rightPush(anyString(), any(LlmMessage.class))).thenReturn(1L);
        when(listOps.size(anyString())).thenReturn(1L);
        ArgumentCaptor<SfChatMessage> captor = ArgumentCaptor.forClass(SfChatMessage.class);

        memoryStore.saveMessage("conv-1", new LlmMessage("user", "hello"));

        verify(chatMessageMapper).insert(captor.capture());
        assertEquals(0, captor.getValue().getTurnIndex());
    }

    @Test
    void fiftySixthMessageGetsTurnIndexFiftyFive() {
        SfChatMessage latest = createDbMessage("conv-1", 54, "assistant", "prev");
        when(chatMessageMapper.selectOne(any())).thenReturn(latest);
        when(listOps.rightPush(anyString(), any(LlmMessage.class))).thenReturn(51L);
        when(listOps.size(anyString())).thenReturn(51L);
        ArgumentCaptor<SfChatMessage> captor = ArgumentCaptor.forClass(SfChatMessage.class);

        memoryStore.saveMessage("conv-1", new LlmMessage("user", "56th"));

        verify(chatMessageMapper).insert(captor.capture());
        assertEquals(55, captor.getValue().getTurnIndex());
    }

    @Test
    void databaseInsertFailureDoesNotWriteToRedis() {
        when(chatMessageMapper.selectOne(any())).thenReturn(null);
        doThrow(new RuntimeException("DB down")).when(chatMessageMapper).insert(any(SfChatMessage.class));

        assertThrows(RuntimeException.class, () ->
                memoryStore.saveMessage("conv-1", new LlmMessage("user", "hello")));

        verify(listOps, never()).rightPush(anyString(), any(LlmMessage.class));
    }

    @Test
    void redisFailureAfterPgInsertDoesNotThrow() {
        when(chatMessageMapper.selectOne(any())).thenReturn(null);
        when(listOps.rightPush(anyString(), any(LlmMessage.class))).thenThrow(new RuntimeException("Redis down"));

        assertDoesNotThrow(() ->
                memoryStore.saveMessage("conv-1", new LlmMessage("user", "hello")));

        verify(chatMessageMapper).insert(any(SfChatMessage.class));
    }

    @Test
    void loadMessagesFallbackUsesRightPushAll() {
        when(listOps.range(anyString(), eq(0L), eq(-1L))).thenReturn(null);
        List<SfChatMessage> dbMessages = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String encContent = tenantKeyService.encrypt("msg" + i, "test-tenant");
            dbMessages.add(createDbMessage("conv-1", i, "user", encContent));
        }
        when(chatMessageMapper.selectList(any())).thenReturn(dbMessages);

        memoryStore.loadMessages("conv-1");

        verify(listOps, times(1)).rightPushAll(anyString(), any(LlmMessage[].class));
    }

    private SfChatMessage createDbMessage(String convId, int turnIndex, String role, String content) {
        SfChatMessage msg = new SfChatMessage();
        msg.setConversationId(convId);
        msg.setTurnIndex(turnIndex);
        msg.setRole(role);
        msg.setContent(content);
        return msg;
    }

    // ------------------------------------------------------------------
    // MemoryStrategy integration tests
    // ------------------------------------------------------------------

    @Test
    void loadMessagesWithBudgetReturnsAllWhenStrategyIsNull() {
        when(listOps.range(anyString(), eq(0L), eq(-1L))).thenReturn(null);
        when(chatMessageMapper.selectList(any())).thenReturn(null);

        List<LlmMessage> result = memoryStore.loadMessages("conv-1", new TokenBudget(1000, 1000));

        assertTrue(result.isEmpty());
    }

    @Test
    void loadMessagesWithBudgetAppliesStrategyWhenConfigured() {
        String encMsg1 = tenantKeyService.encrypt("hello", "test-tenant");
        String encMsg2 = tenantKeyService.encrypt("world", "test-tenant");
        List<LlmMessage> redisMessages = List.of(
                new LlmMessage("user", encMsg1),
                new LlmMessage("assistant", encMsg2)
        );
        when(listOps.range(anyString(), eq(0L), eq(-1L))).thenReturn(redisMessages);
        when(listOps.size(anyString())).thenReturn(2L);

        SlidingWindowStrategy strategy = new SlidingWindowStrategy(25);
        memoryStore.setMemoryStrategy(strategy);

        TokenBudget budget = new TokenBudget(10000, 10000);
        List<LlmMessage> result = memoryStore.loadMessages("conv-1", budget);

        assertEquals(2, result.size());
        assertEquals("hello", result.get(0).getContent());
        assertEquals("world", result.get(1).getContent());
    }

    @Test
    void loadMessagesWithBudgetDropsOldMessagesWhenBudgetTight() {
        String encMsg1 = tenantKeyService.encrypt(
            "first long message that costs many tokens because it is very lengthy indeed", "test-tenant");
        String encMsg2 = tenantKeyService.encrypt(
            "second message also quite long to consume token budget quickly", "test-tenant");
        String encMsg3 = tenantKeyService.encrypt(
            "third message is also long enough to matter for token counting", "test-tenant");
        List<LlmMessage> redisMessages = List.of(
                new LlmMessage("user", encMsg1),
                new LlmMessage("assistant", encMsg2),
                new LlmMessage("user", encMsg3)
        );
        when(listOps.range(anyString(), eq(0L), eq(-1L))).thenReturn(redisMessages);
        when(listOps.size(anyString())).thenReturn(3L);

        SlidingWindowStrategy strategy = new SlidingWindowStrategy(25);
        memoryStore.setMemoryStrategy(strategy);

        TokenBudget budget = new TokenBudget(15, 15);
        List<LlmMessage> result = memoryStore.loadMessages("conv-1", budget);

        assertTrue(result.size() < 3, "Should drop oldest messages due to budget");
        assertEquals("third message is also long enough to matter for token counting",
            result.get(result.size() - 1).getContent());
    }

    @Test
    void setMemoryStrategyUpdatesStrategy() {
        SlidingWindowStrategy strategy = new SlidingWindowStrategy(50);
        memoryStore.setMemoryStrategy(strategy);

        assertSame(strategy, memoryStore.getMemoryStrategy());
    }

    @Test
    void getMemoryStrategyReturnsNullByDefault() {
        assertNull(memoryStore.getMemoryStrategy());
    }

    @Test
    void loadMessagesWithBudgetReturnsEmptyWhenNoMessages() {
        when(listOps.range(anyString(), eq(0L), eq(-1L))).thenReturn(null);
        when(chatMessageMapper.selectList(any())).thenReturn(null);

        SlidingWindowStrategy strategy = new SlidingWindowStrategy(25);
        memoryStore.setMemoryStrategy(strategy);

        List<LlmMessage> result = memoryStore.loadMessages("conv-1", new TokenBudget(1000, 1000));

        assertTrue(result.isEmpty());
    }
}
