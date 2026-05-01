package com.schemaplexai.agent.engine.tool;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ToolSafetyGuardObfuscationTest {

    private final ToolSafetyGuard guard = new ToolSafetyGuard();

    @Test
    void shouldBlockUnicodeHomoglyphDrop() {
        // Cyrillic 'о' (U+043E) instead of Latin 'o'
        String obfuscated = "DRоP TABLE users";
        ToolSafetyGuard.SafetyCheckResult result = guard.check("executeSql", obfuscated);
        assertTrue(result.blocked(), "Should detect Unicode homoglyph obfuscation");
    }

    @Test
    void shouldBlockHtmlEncodedDrop() {
        String obfuscated = "&#68;ROP TABLE users";
        ToolSafetyGuard.SafetyCheckResult result = guard.check("executeSql", obfuscated);
        assertTrue(result.blocked(), "Should detect HTML entity encoding");
    }

    @Test
    void shouldBlockJsonEscapedDrop() {
        String obfuscated = "DROP\\u0020TABLE users";
        ToolSafetyGuard.SafetyCheckResult result = guard.check("executeSql", obfuscated);
        assertTrue(result.blocked(), "Should detect JSON escape sequences");
    }

    @Test
    void shouldBlockWhitespaceCollapsedDrop() {
        String obfuscated = "DROP   TABLE   users";
        ToolSafetyGuard.SafetyCheckResult result = guard.check("executeSql", obfuscated);
        assertTrue(result.blocked(), "Should detect with extra whitespace");
    }

    @Test
    void shouldBlockNewlineSeparatedDrop() {
        String obfuscated = "DROP\nTABLE users";
        ToolSafetyGuard.SafetyCheckResult result = guard.check("executeSql", obfuscated);
        assertTrue(result.blocked(), "Should detect newline-separated commands");
    }

    @Test
    void shouldBlockTabSeparatedDrop() {
        String obfuscated = "DROP\tTABLE users";
        ToolSafetyGuard.SafetyCheckResult result = guard.check("executeSql", obfuscated);
        assertTrue(result.blocked(), "Should detect tab-separated commands");
    }

    @Test
    void shouldAllowNormalSelect() {
        ToolSafetyGuard.SafetyCheckResult result = guard.check("executeSql", "SELECT * FROM users");
        assertTrue(result.allowed());
    }
}
