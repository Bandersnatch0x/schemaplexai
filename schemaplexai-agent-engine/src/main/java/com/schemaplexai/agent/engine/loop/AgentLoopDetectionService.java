package com.schemaplexai.agent.engine.loop;

import com.schemaplexai.agent.engine.model.LlmMessage;
import com.schemaplexai.agent.engine.memory.CompositeChatMemoryStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentLoopDetectionService {

    private final CompositeChatMemoryStore chatMemoryStore;

    public boolean detectLoop(String conversationId) {
        List<LlmMessage> messages = chatMemoryStore.loadMessages(conversationId);
        if (messages.size() < 6) {
            return false;
        }
        Set<String> seen = new HashSet<>();
        int duplicateCount = 0;
        for (LlmMessage msg : messages) {
            String key = msg.getRole() + ":" + msg.getContent();
            if (!seen.add(key)) {
                duplicateCount++;
            }
        }
        boolean loopDetected = duplicateCount >= 3;
        if (loopDetected) {
            log.warn("Loop detected in conversation {} with {} duplicate messages", conversationId, duplicateCount);
        }
        return loopDetected;
    }
}
