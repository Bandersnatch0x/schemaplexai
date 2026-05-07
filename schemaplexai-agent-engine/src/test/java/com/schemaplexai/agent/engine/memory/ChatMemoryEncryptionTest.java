package com.schemaplexai.agent.engine.memory;

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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for AES-256-GCM encryption in CompositeChatMemoryStore.
 *
 * Verifies:
 * - Content is encrypted before persisting to PostgreSQL (not plaintext)
 * - Content is encrypted before persisting to Redis (not plaintext)
 * - Content is correctly decrypted when loaded
 * - Different tenants use different encryption keys
 */
@ExtendWith(MockitoExtension.class)
class ChatMemoryEncryptionTest {

    private static final String TENANT_A = "tenant-a";
    private static final String TENANT_B = "tenant-b";
    private static final String PLAINTEXT = "My password is secret123";

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

        // Use a test master secret — in production this comes from vault/config
        tenantKeyService = new TenantKeyService("test-master-secret-for-unit-tests-32bytes!");
        memoryStore = new CompositeChatMemoryStore(redisTemplate, stringRedisTemplate, chatMessageMapper, tenantKeyService);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void shouldStoreEncryptedContentInDatabase() {
        // Arrange
        TenantContextHolder.setTenantId(TENANT_A);
        when(chatMessageMapper.selectOne(any())).thenReturn(null);
        when(listOps.rightPush(anyString(), any(LlmMessage.class))).thenReturn(1L);
        when(listOps.size(anyString())).thenReturn(1L);
        ArgumentCaptor<SfChatMessage> captor = ArgumentCaptor.forClass(SfChatMessage.class);

        LlmMessage msg = new LlmMessage("user", PLAINTEXT);

        // Act
        memoryStore.saveMessage("test-conv", msg);

        // Assert — content saved to PG must NOT be plaintext
        verify(chatMessageMapper).insert(captor.capture());
        String savedContent = captor.getValue().getContent();
        assertFalse(savedContent.contains("secret123"),
                "Content stored in database should be encrypted, not plaintext");
        assertNotEquals(PLAINTEXT, savedContent,
                "Encrypted content should differ from plaintext");
    }

    @Test
    void shouldStoreEncryptedContentInRedis() {
        // Arrange
        TenantContextHolder.setTenantId(TENANT_A);
        when(chatMessageMapper.selectOne(any())).thenReturn(null);
        when(listOps.rightPush(anyString(), any(LlmMessage.class))).thenReturn(1L);
        when(listOps.size(anyString())).thenReturn(1L);
        ArgumentCaptor<LlmMessage> redisCaptor = ArgumentCaptor.forClass(LlmMessage.class);

        LlmMessage msg = new LlmMessage("user", PLAINTEXT);

        // Act
        memoryStore.saveMessage("test-conv", msg);

        // Assert — content stored in Redis must NOT be plaintext
        verify(listOps).rightPush(anyString(), redisCaptor.capture());
        String redisContent = redisCaptor.getValue().getContent();
        assertFalse(redisContent.contains("secret123"),
                "Content stored in Redis should be encrypted, not plaintext");
    }

    @Test
    void shouldLoadDecryptedContentFromDatabase() {
        // Arrange
        TenantContextHolder.setTenantId(TENANT_A);
        when(listOps.range(anyString(), eq(0L), eq(-1L))).thenReturn(null);

        // Simulate PG returning encrypted content
        String encryptedContent = tenantKeyService.encrypt(PLAINTEXT, TENANT_A);
        SfChatMessage dbMsg = new SfChatMessage();
        dbMsg.setConversationId("test-conv");
        dbMsg.setTurnIndex(0);
        dbMsg.setRole("user");
        dbMsg.setContent(encryptedContent);
        when(chatMessageMapper.selectList(any())).thenReturn(List.of(dbMsg));

        // Act
        List<LlmMessage> loaded = memoryStore.loadMessages("test-conv");

        // Assert — content should be decrypted
        assertEquals(1, loaded.size());
        assertEquals(PLAINTEXT, loaded.get(0).getContent());
    }

    @Test
    void shouldLoadDecryptedContentFromRedis() {
        // Arrange
        TenantContextHolder.setTenantId(TENANT_A);
        String encryptedContent = tenantKeyService.encrypt(PLAINTEXT, TENANT_A);
        LlmMessage encryptedMsg = new LlmMessage("user", encryptedContent);
        when(listOps.range(anyString(), eq(0L), eq(-1L))).thenReturn(List.of(encryptedMsg));
        when(listOps.size(anyString())).thenReturn(1L);

        // Act
        List<LlmMessage> loaded = memoryStore.loadMessages("test-conv");

        // Assert — content should be decrypted
        assertEquals(1, loaded.size());
        assertEquals(PLAINTEXT, loaded.get(0).getContent());
    }

    @Test
    void shouldUseDifferentKeysForDifferentTenants() {
        // Arrange
        TenantContextHolder.setTenantId(TENANT_A);
        when(chatMessageMapper.selectOne(any())).thenReturn(null);
        when(listOps.rightPush(anyString(), any(LlmMessage.class))).thenReturn(1L);
        when(listOps.size(anyString())).thenReturn(1L);

        // Save same plaintext for tenant A
        memoryStore.saveMessage("conv-a", new LlmMessage("user", PLAINTEXT));

        ArgumentCaptor<SfChatMessage> captorA = ArgumentCaptor.forClass(SfChatMessage.class);
        verify(chatMessageMapper).insert(captorA.capture());
        String encryptedA = captorA.getValue().getContent();

        // Save same plaintext for tenant B
        TenantContextHolder.setTenantId(TENANT_B);
        reset(chatMessageMapper);
        when(chatMessageMapper.selectOne(any())).thenReturn(null);
        memoryStore.saveMessage("conv-b", new LlmMessage("user", PLAINTEXT));

        ArgumentCaptor<SfChatMessage> captorB = ArgumentCaptor.forClass(SfChatMessage.class);
        verify(chatMessageMapper).insert(captorB.capture());
        String encryptedB = captorB.getValue().getContent();

        // Assert — same plaintext, different tenants => different ciphertext
        assertNotEquals(encryptedA, encryptedB,
                "Different tenants should produce different ciphertext for the same plaintext");
    }

    @Test
    void shouldProduceDifferentCiphertextForSameMessage() {
        // Arrange — GCM uses random IV, so same message encrypted twice should differ
        TenantContextHolder.setTenantId(TENANT_A);
        when(chatMessageMapper.selectOne(any())).thenReturn(null);
        when(listOps.rightPush(anyString(), any(LlmMessage.class))).thenReturn(1L);
        when(listOps.size(anyString())).thenReturn(1L);

        // First save
        memoryStore.saveMessage("conv-1", new LlmMessage("user", PLAINTEXT));
        ArgumentCaptor<SfChatMessage> captor1 = ArgumentCaptor.forClass(SfChatMessage.class);
        verify(chatMessageMapper).insert(captor1.capture());

        // Second save
        reset(chatMessageMapper);
        when(chatMessageMapper.selectOne(any())).thenReturn(null);
        memoryStore.saveMessage("conv-2", new LlmMessage("user", PLAINTEXT));
        ArgumentCaptor<SfChatMessage> captor2 = ArgumentCaptor.forClass(SfChatMessage.class);
        verify(chatMessageMapper).insert(captor2.capture());

        // Assert — random IV ensures different ciphertext each time
        assertNotEquals(captor1.getValue().getContent(), captor2.getValue().getContent(),
                "Same plaintext encrypted twice should produce different ciphertext (random IV)");
    }
}
