package com.schemaplexai.agent.engine.memory;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ConversationFileTracker {

    private final StringRedisTemplate redisTemplate;
    private static final String KEY_PREFIX = "sf:files:";
    private static final int MAX_ENTRIES = 20;

    public void trackFile(String conversationId, String filePath) {
        String key = KEY_PREFIX + conversationId;
        redisTemplate.opsForList().leftPush(key, filePath);
        redisTemplate.opsForList().trim(key, 0, MAX_ENTRIES - 1);
    }

    public List<String> getRecentFiles(String conversationId, int limit) {
        String key = KEY_PREFIX + conversationId;
        Long size = redisTemplate.opsForList().size(key);
        if (size == null || size == 0) {
            return List.of();
        }
        int end = Math.min(limit, size.intValue()) - 1;
        List<String> files = redisTemplate.opsForList().range(key, 0, end);
        return files != null ? files : List.of();
    }
}
