package com.schemaplexai.agent.engine.memory;

import com.schemaplexai.agent.engine.model.LlmMessage;
import com.schemaplexai.common.constants.CommonConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CompositeChatMemoryStore {

    private static final long MAX_MESSAGES = 50;
    private static final Duration CHAT_HISTORY_TTL = Duration.ofDays(7);

    private final RedisTemplate<String, LlmMessage> redisTemplate;

    public List<LlmMessage> loadMessages(String conversationId) {
        String key = String.format(CommonConstants.REDIS_KEY_CHAT_MEMORY, conversationId);
        List<LlmMessage> messages = redisTemplate.opsForList().range(key, 0, -1);
        if (messages == null) {
            return new ArrayList<>();
        }
        Long size = redisTemplate.opsForList().size(key);
        if (size != null && size > MAX_MESSAGES) {
            redisTemplate.opsForList().trim(key, size - MAX_MESSAGES, -1);
        }
        return new ArrayList<>(messages);
    }

    public void saveMessage(String conversationId, LlmMessage message) {
        String key = String.format(CommonConstants.REDIS_KEY_CHAT_MEMORY, conversationId);
        redisTemplate.opsForList().rightPush(key, message);
        redisTemplate.expire(key, CHAT_HISTORY_TTL);
        Long size = redisTemplate.opsForList().size(key);
        if (size != null && size > MAX_MESSAGES) {
            redisTemplate.opsForList().trim(key, size - MAX_MESSAGES, -1);
        }
    }

    public void clear(String conversationId) {
        String key = String.format(CommonConstants.REDIS_KEY_CHAT_MEMORY, conversationId);
        redisTemplate.delete(key);
    }
}
