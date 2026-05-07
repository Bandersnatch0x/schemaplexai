package com.schemaplexai.agent.engine.memory;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.schemaplexai.agent.engine.entity.SfChatMessage;
import com.schemaplexai.agent.engine.mapper.SfChatMessageMapper;
import com.schemaplexai.agent.engine.model.LlmMessage;
import com.schemaplexai.common.constants.CommonConstants;
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

    public List<LlmMessage> loadMessages(String conversationId) {
        String key = String.format(CommonConstants.REDIS_KEY_CHAT_MEMORY, conversationId);
        List<LlmMessage> messages = redisTemplate.opsForList().range(key, 0, -1);
        if (messages != null && !messages.isEmpty()) {
            Long size = redisTemplate.opsForList().size(key);
            if (size != null && size > MAX_MESSAGES) {
                redisTemplate.opsForList().trim(key, size - MAX_MESSAGES, -1);
            }
            return new ArrayList<>(messages);
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
                return new ArrayList<>(retryMessages);
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
                LlmMessage msg = new LlmMessage(dbMsg.getRole(), dbMsg.getContent());
                result.add(msg);
            }
            // Backfill L1 in a single batch
            if (!result.isEmpty()) {
                redisTemplate.opsForList().rightPushAll(key, result.toArray(new LlmMessage[0]));
                redisTemplate.expire(key, CHAT_HISTORY_TTL);
            }
        }
        return result;
    }

    public void saveMessage(String conversationId, LlmMessage message) {
        // Serialize per-conversation to prevent duplicate turnIndex (M-2 fix)
        Object lock = conversationLocks.computeIfAbsent(conversationId, k -> new Object());
        Integer turnIndex;
        synchronized (lock) {
            turnIndex = computeNextTurnIndex(conversationId);

            // L2: PostgreSQL first
            SfChatMessage entity = new SfChatMessage();
            entity.setConversationId(conversationId);
            entity.setTurnIndex(turnIndex);
            entity.setRole(message.getRole());
            entity.setContent(message.getContent());
            chatMessageMapper.insert(entity);
        }

        // L1: Redis after successful PG insert
        String key = String.format(CommonConstants.REDIS_KEY_CHAT_MEMORY, conversationId);
        try {
            redisTemplate.opsForList().rightPush(key, message);
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

    public void clear(String conversationId) {
        String key = String.format(CommonConstants.REDIS_KEY_CHAT_MEMORY, conversationId);
        redisTemplate.delete(key);

        chatMessageMapper.delete(
                new LambdaQueryWrapper<SfChatMessage>()
                        .eq(SfChatMessage::getConversationId, conversationId)
        );
    }
}
