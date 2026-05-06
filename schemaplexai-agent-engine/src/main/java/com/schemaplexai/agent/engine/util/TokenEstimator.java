package com.schemaplexai.agent.engine.util;

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
}
