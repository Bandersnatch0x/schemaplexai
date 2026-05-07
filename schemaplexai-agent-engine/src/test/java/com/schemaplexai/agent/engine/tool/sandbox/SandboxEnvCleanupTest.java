package com.schemaplexai.agent.engine.tool.sandbox;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SandboxEnvCleanupTest {

    @Test
    void shouldRemovePasswordFromEnv() {
        Map<String, String> env = Map.of(
                "HOME", "/home/user",
                "SPRING_DATASOURCE_PASSWORD", "supersecret",
                "MY_SECRET_KEY", "key123",
                "APP_TOKEN", "tok456",
                "NORMAL_VAR", "safe"
        );
        String[] sanitized = EnvSanitizer.sanitize(env);
        Map<String, String> result = parseEnvArray(sanitized);

        assertEquals("/home/user", result.get("HOME"));
        assertEquals("safe", result.get("NORMAL_VAR"));
        assertNull(result.get("SPRING_DATASOURCE_PASSWORD"));
        assertNull(result.get("MY_SECRET_KEY"));
        assertNull(result.get("APP_TOKEN"));
    }

    @Test
    void shouldHandleNullEnv() {
        String[] sanitized = EnvSanitizer.sanitize(null);
        assertEquals(0, sanitized.length);
    }

    @Test
    void shouldKeepAllNonSensitiveKeys() {
        Map<String, String> env = Map.of("PATH", "/usr/bin", "HOME", "/root");
        String[] sanitized = EnvSanitizer.sanitize(env);
        assertEquals(2, sanitized.length);
    }

    private Map<String, String> parseEnvArray(String[] arr) {
        Map<String, String> map = new HashMap<>();
        for (String entry : arr) {
            int eq = entry.indexOf('=');
            map.put(entry.substring(0, eq), entry.substring(eq + 1));
        }
        return map;
    }
}
