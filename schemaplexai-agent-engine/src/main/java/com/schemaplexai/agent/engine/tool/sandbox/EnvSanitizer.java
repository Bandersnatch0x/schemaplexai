package com.schemaplexai.agent.engine.tool.sandbox;

import java.util.Map;

/**
 * Strips sensitive environment variables before passing them to sandbox processes.
 *
 * <p>Variables whose upper-cased key contains PASSWORD, SECRET, TOKEN, or _KEY
 * are excluded. This prevents credential leakage to child processes.
 */
public final class EnvSanitizer {

    private EnvSanitizer() {}

    /**
     * Sanitize a map of environment variables, returning only non-sensitive entries
     * as a String array in {@code KEY=VALUE} format suitable for process builders.
     *
     * @param env environment variable map; may be null
     * @return array of sanitized env entries; empty if input is null
     */
    public static String[] sanitize(Map<String, String> env) {
        if (env == null) {
            return new String[0];
        }
        return env.entrySet().stream()
                .filter(e -> !isSensitiveKey(e.getKey()))
                .map(e -> e.getKey() + "=" + e.getValue())
                .toArray(String[]::new);
    }

    /**
     * Returns true if the key looks sensitive based on common naming patterns.
     */
    public static boolean isSensitiveKey(String key) {
        String upper = key.toUpperCase();
        return upper.contains("PASSWORD") || upper.contains("SECRET")
                || upper.contains("TOKEN") || upper.contains("_KEY");
    }
}
