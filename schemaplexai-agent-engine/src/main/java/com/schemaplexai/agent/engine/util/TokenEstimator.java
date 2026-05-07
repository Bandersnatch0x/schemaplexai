package com.schemaplexai.agent.engine.util;

import com.schemaplexai.agent.engine.model.LlmMessage;
import java.util.List;

/**
 * Token estimation utility. Rough heuristic: 1 token ~ 4 characters.
 */
public final class TokenEstimator {

    private TokenEstimator() {}

    public static long estimate(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return Math.max(1, text.length() / 4L);
    }

    public static long estimate(List<LlmMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return 0;
        }
        long total = 0;
        for (LlmMessage msg : messages) {
            total += estimate(msg.getContent());
        }
        return total;
    }
}
