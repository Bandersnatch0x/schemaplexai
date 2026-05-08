package com.schemaplexai.agent.engine.memory;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.schemaplexai.agent.engine.admission.TokenBudget;
import com.schemaplexai.agent.engine.entity.SfChatMessage;
import com.schemaplexai.agent.engine.mapper.SfChatMessageMapper;
import com.schemaplexai.agent.engine.model.LlmMessage;
import com.schemaplexai.common.constants.CommonConstants;
import com.schemaplexai.common.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class CompositeChatMemoryStore {

    private static final long MAX_MESSAGES = 50;
    private static final Duration CHAT_HISTORY_TTL = Duration.ofDays(7);
    private static final Duration BACKFILL_LOCK_TTL = Duration.ofSeconds(5);
    private static final long BACKFILL_WAIT_MS = 50;

    /** Per-conversation lock objects to serialize turnIndex computation (M-2 fix). */
    private final ConcurrentHashMap<String, Object> conversationLocks = new ConcurrentHashMap<>();

    private final RedisTemplate<String, LlmMessage> redisTemplate;
    private final org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate;
    private final SfChatMessageMapper chatMessageMapper;
    private final TenantKeyService tenantKeyService;
    private MemoryStrategy memoryStrategy;

    public List<LlmMessage> loadMessages(String conversationId) {
        String tenantId = TenantContextHolder.getTenantId();
        String key = String.format(CommonConstants.REDIS_KEY_CHAT_MEMORY, conversationId);
        List<LlmMessage> messages = redisTemplate.opsForList().range(key, 0, -1);
        if (messages != null && !messages.isEmpty()) {
            Long size = redisTemplate.opsForList().size(key);
            if (size != null && size > MAX_MESSAGES) {
                redisTemplate.opsForList().trim(key, size - MAX_MESSAGES, -1);
            }
            return decryptMessages(messages, tenantId);
        }

        // L1 miss: try to acquire backfill lock to prevent duplicate PG reads (M-3 fix)
        String lockKey = key + ":backfill_lock";
        Boolean acquired = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", BACKFILL_LOCK_TTL);
        if (Boolean.FALSE.equals(acquired)) {
            // Another thread/process is backfilling — wait briefly and retry from Redis
            try {
                Thread.sleep(BACKFILL_WAIT_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            List<LlmMessage> retryMessages = redisTemplate.opsForList().range(key, 0, -1);
            if (retryMessages != null && !retryMessages.isEmpty()) {
                return decryptMessages(retryMessages, tenantId);
            }
        }

        // L1 miss: fallback to L2 (PostgreSQL)
        List<SfChatMessage> dbMessages = chatMessageMapper.selectList(
                new LambdaQueryWrapper<SfChatMessage>()
                        .eq(SfChatMessage::getConversationId, conversationId)
                        .orderByAsc(SfChatMessage::getTurnIndex)
                        .last("LIMIT " + MAX_MESSAGES)
        );

        List<LlmMessage> result = new ArrayList<>();
        if (dbMessages != null) {
            for (SfChatMessage dbMsg : dbMessages) {
                String decryptedContent = tenantKeyService.decrypt(dbMsg.getContent(), tenantId);
                LlmMessage msg = new LlmMessage(dbMsg.getRole(), decryptedContent);
                result.add(msg);
            }
            // Backfill L1 with encrypted content (same format as what was stored)
            if (!result.isEmpty()) {
                List<LlmMessage> encryptedForCache = new ArrayList<>();
                for (SfChatMessage dbMsg : dbMessages) {
                    encryptedForCache.add(new LlmMessage(dbMsg.getRole(), dbMsg.getContent()));
                }
                redisTemplate.opsForList().rightPushAll(key, encryptedForCache.toArray(new LlmMessage[0]));
                redisTemplate.expire(key, CHAT_HISTORY_TTL);
            }
        }
        return result;
    }

    /**
     * Decrypt a list of messages loaded from Redis (stored in encrypted form).
     */
    private List<LlmMessage> decryptMessages(List<LlmMessage> encrypted, String tenantId) {
        List<LlmMessage> decrypted = new ArrayList<>(encrypted.size());
        for (LlmMessage msg : encrypted) {
            decrypted.add(new LlmMessage(msg.getRole(), tenantKeyService.decrypt(msg.getContent(), tenantId)));
        }
        return decrypted;
    }

    public void saveMessage(String conversationId, LlmMessage message) {
        String tenantId = TenantContextHolder.getTenantId();
        String encryptedContent = tenantKeyService.encrypt(message.getContent(), tenantId);

        // Serialize per-conversation to prevent duplicate turnIndex (M-2 fix)
        Object lock = conversationLocks.computeIfAbsent(conversationId, k -> new Object());
        Integer turnIndex;
        synchronized (lock) {
            turnIndex = computeNextTurnIndex(conversationId);

            // L2: PostgreSQL first — store encrypted content
            SfChatMessage entity = new SfChatMessage();
            entity.setConversationId(conversationId);
            entity.setTurnIndex(turnIndex);
            entity.setRole(message.getRole());
            entity.setContent(encryptedContent);
            chatMessageMapper.insert(entity);
        }

        // L1: Redis after successful PG insert — store encrypted content
        LlmMessage encryptedMsg = new LlmMessage(message.getRole(), encryptedContent);
        String key = String.format(CommonConstants.REDIS_KEY_CHAT_MEMORY, conversationId);
        try {
            redisTemplate.opsForList().rightPush(key, encryptedMsg);
            redisTemplate.expire(key, CHAT_HISTORY_TTL);
            Long size = redisTemplate.opsForList().size(key);
            if (size != null && size > MAX_MESSAGES) {
                redisTemplate.opsForList().trim(key, size - MAX_MESSAGES, -1);
            }
        } catch (Exception e) {
            log.warn("Failed to update Redis cache for conversation {} after PG insert. L2 remains source of truth.", conversationId, e);
        }
    }

    private Integer computeNextTurnIndex(String conversationId) {
        SfChatMessage latest = chatMessageMapper.selectOne(
                new LambdaQueryWrapper<SfChatMessage>()
                        .eq(SfChatMessage::getConversationId, conversationId)
                        .orderByDesc(SfChatMessage::getTurnIndex)
                        .last("LIMIT 1")
        );
        return latest != null ? latest.getTurnIndex() + 1 : 0;
    }

    /**
     * Load messages with memory strategy applied for token-budget-aware selection.
     * Falls back to plain loadMessages if no strategy is configured.
     *
     * @param conversationId conversation identifier
     * @param budget         token budget constraint
     * @return selected messages fitting within the budget
     */
    public List<LlmMessage> loadMessages(String conversationId, TokenBudget budget) {
        List<LlmMessage> allMessages = loadMessages(conversationId);
        if (memoryStrategy == null || allMessages.isEmpty()) {
            return allMessages;
        }

        List<ChatMessage> chatMessages = toChatMessages(allMessages);
        List<ChatMessage> selected = memoryStrategy.select(chatMessages, budget);
        return toLlmMessages(selected);
    }

    /**
     * Set the memory strategy for token-budget-aware message selection.
     *
     * @param memoryStrategy the strategy to apply (nullable)
     */
    public void setMemoryStrategy(MemoryStrategy memoryStrategy) {
        this.memoryStrategy = memoryStrategy;
    }

    /**
     * Get the currently configured memory strategy.
     *
     * @return current strategy, or null if none configured
     */
    public MemoryStrategy getMemoryStrategy() {
        return memoryStrategy;
    }

    private List<ChatMessage> toChatMessages(List<LlmMessage> messages) {
        List<ChatMessage> result = new ArrayList<>(messages.size());
        for (LlmMessage msg : messages) {
            result.add(new ChatMessage(msg.getRole(), msg.getContent()));
        }
        return result;
    }

    private List<LlmMessage> toLlmMessages(List<ChatMessage> messages) {
        List<LlmMessage> result = new ArrayList<>(messages.size());
        for (ChatMessage msg : messages) {
            result.add(new LlmMessage(msg.getRole(), msg.getContent()));
        }
        return result;
    }

    public void clear(String conversationId) {
        String key = String.format(CommonConstants.REDIS_KEY_CHAT_MEMORY, conversationId);
        redisTemplate.delete(key);

        chatMessageMapper.delete(
                new LambdaQueryWrapper<SfChatMessage>()
                        .eq(SfChatMessage::getConversationId, conversationId)
        );
    }

    /**
     * Replace all messages for a conversation (used for compaction / summarization).
     * Deletes Redis cache and stores new messages in both PG and Redis.
     */
    public void replaceMessages(String conversationId, List<LlmMessage> messages) {
        if (conversationId == null || conversationId.isBlank()) {
            return;
        }
        String tenantId = TenantContextHolder.getTenantId();
        String key = String.format(CommonConstants.REDIS_KEY_CHAT_MEMORY, conversationId);

        redisTemplate.delete(key);

        if (messages == null || messages.isEmpty()) {
            return;
        }

        List<LlmMessage> encrypted = new ArrayList<>(messages.size());
        for (LlmMessage msg : messages) {
            encrypted.add(new LlmMessage(msg.getRole(), tenantKeyService.encrypt(msg.getContent(), tenantId)));
        }
        redisTemplate.opsForList().rightPushAll(key, encrypted.toArray(new LlmMessage[0]));
        redisTemplate.expire(key, CHAT_HISTORY_TTL);
    }

    /**
     * Rough token estimate: ~4 characters per token (approximation for CJK + ASCII mix).
     */
    public int estimateTokens(List<LlmMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return 0;
        }
        int totalChars = 0;
        for (LlmMessage msg : messages) {
            totalChars += msg.getRole().length();
            totalChars += msg.getContent().length();
            totalChars += 4; // overhead per message
        }
        return Math.max(1, totalChars / 4);
    }
}
