package com.schemaplexai.agent.engine.tool;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ToolSafetyGuardTest {

    private ToolSafetyGuard guard;

    @BeforeEach
    void setUp() {
        guard = new ToolSafetyGuard();
    }

    // --- Irreversible tool name blocking ---

    @Test
    void shouldBlockVolumeDelete() {
        ToolSafetyGuard.SafetyCheckResult result = guard.check("volumeDelete", "{\"id\":\"vol-123\"}");
        assertFalse(result.allowed());
        assertTrue(result.blocked());
        assertEquals(ToolErrorCategory.IRREVERSIBLE_OPERATION, result.errorCategory());
        assertEquals("Operation blocked by safety policy.", result.reason());
    }

    @Test
    void shouldBlockDatabaseDrop() {
        ToolSafetyGuard.SafetyCheckResult result = guard.check("databaseDrop", "{\"name\":\"users_db\"}");
        assertFalse(result.allowed());
        assertTrue(result.blocked());
        assertEquals(ToolErrorCategory.IRREVERSIBLE_OPERATION, result.errorCategory());
    }

    @Test
    void shouldBlockDestroy() {
        ToolSafetyGuard.SafetyCheckResult result = guard.check("destroy", "{\"id\":\"res-1\"}");
        assertFalse(result.allowed());
        assertTrue(result.blocked());
    }

    @Test
    void shouldBlockPurge() {
        ToolSafetyGuard.SafetyCheckResult result = guard.check("purge", "{\"cache\":\"all\"}");
        assertFalse(result.allowed());
        assertTrue(result.blocked());
    }

    @Test
    void shouldBlockHardDelete() {
        ToolSafetyGuard.SafetyCheckResult result = guard.check("hardDelete", "{\"id\":\"123\"}");
        assertFalse(result.allowed());
        assertTrue(result.blocked());
    }

    @Test
    void shouldBlockPermanentDelete() {
        ToolSafetyGuard.SafetyCheckResult result = guard.check("permanentDelete", "{\"id\":\"456\"}");
        assertFalse(result.allowed());
        assertTrue(result.blocked());
    }

    // --- Irreversible command pattern in arguments ---

    @Test
    void shouldBlockDropTableInArguments() {
        ToolSafetyGuard.SafetyCheckResult result = guard.check("executeSql", "DROP TABLE users");
        assertFalse(result.allowed());
        assertTrue(result.blocked());
        assertEquals(ToolErrorCategory.IRREVERSIBLE_OPERATION, result.errorCategory());
    }

    @Test
    void shouldBlockDropDatabaseInArguments() {
        ToolSafetyGuard.SafetyCheckResult result = guard.check("executeSql", "DROP DATABASE production");
        assertFalse(result.allowed());
        assertTrue(result.blocked());
    }

    @Test
    void shouldBlockDeleteFromInArguments() {
        ToolSafetyGuard.SafetyCheckResult result = guard.check("executeSql", "DELETE FROM users WHERE id = 1");
        assertFalse(result.allowed());
        assertTrue(result.blocked());
    }

    @Test
    void shouldBlockTruncateInArguments() {
        ToolSafetyGuard.SafetyCheckResult result = guard.check("executeSql", "TRUNCATE TABLE logs");
        assertFalse(result.allowed());
        assertTrue(result.blocked());
    }

    @Test
    void shouldBlockRmRfInArguments() {
        ToolSafetyGuard.SafetyCheckResult result = guard.check("shellExec", "rm -rf /");
        assertFalse(result.allowed());
        assertTrue(result.blocked());
    }

    @Test
    void shouldBlockCaseInsensitiveDestructiveCommands() {
        ToolSafetyGuard.SafetyCheckResult result = guard.check("executeSql", "drop table users");
        assertFalse(result.allowed(), "Should block lowercase DROP TABLE");
    }

    // --- Safe operations should pass ---

    @Test
    void shouldAllowReadOperations() {
        ToolSafetyGuard.SafetyCheckResult result = guard.check("fileRead", "{\"path\":\"/tmp/log.txt\"}");
        assertTrue(result.allowed());
        assertFalse(result.blocked());
        assertNull(result.errorCategory());
        assertNull(result.reason());
    }

    @Test
    void shouldAllowSafeWriteOperations() {
        ToolSafetyGuard.SafetyCheckResult result = guard.check("fileWrite", "{\"path\":\"/tmp/output.txt\"}");
        assertTrue(result.allowed());
    }

    @Test
    void shouldAllowGenericDeleteOperations() {
        ToolSafetyGuard.SafetyCheckResult result = guard.check("deleteCacheEntry", "{\"key\":\"temp\"}");
        assertTrue(result.allowed(), "Generic delete tool names should not be blocked");
    }

    @Test
    void shouldAllowSelectQueries() {
        ToolSafetyGuard.SafetyCheckResult result = guard.check("executeSql", "SELECT * FROM users");
        assertTrue(result.allowed());
    }

    @Test
    void shouldAllowInsertQueries() {
        ToolSafetyGuard.SafetyCheckResult result = guard.check("executeSql", "INSERT INTO users VALUES (1, 'John')");
        assertTrue(result.allowed());
    }

    @Test
    void shouldAllowUpdateQueries() {
        ToolSafetyGuard.SafetyCheckResult result = guard.check("executeSql", "UPDATE users SET name = 'Jane' WHERE id = 1");
        assertTrue(result.allowed());
    }

    // --- Environment mismatch tests ---

    @Test
    void shouldBlockWhenProdArgumentsInNonProdEnvironment() {
        ToolSafetyGuard.SafetyCheckResult result = guard.check("deploy", "{\"env\":\"production\",\"token\":\"prod-token\"}", "staging");
        assertFalse(result.allowed());
        assertTrue(result.blocked());
        assertEquals(ToolErrorCategory.ENVIRONMENT_MISMATCH, result.errorCategory());
    }

    @Test
    void shouldAllowWhenEnvironmentMatches() {
        ToolSafetyGuard.SafetyCheckResult result = guard.check("deploy", "{\"env\":\"production\"}", "production");
        assertTrue(result.allowed());
    }

    @Test
    void shouldAllowWhenNoProdInArguments() {
        ToolSafetyGuard.SafetyCheckResult result = guard.check("deploy", "{\"env\":\"staging\"}", "staging");
        assertTrue(result.allowed());
    }

    @Test
    void shouldAllowWhenExpectedEnvironmentIsNull() {
        ToolSafetyGuard.SafetyCheckResult result = guard.check("deploy", "{\"env\":\"production\"}", null);
        assertTrue(result.allowed(), "Null expected environment should not block");
    }

    // --- Null handling tests ---

    @Test
    void shouldAllowWhenArgumentsAreNull() {
        ToolSafetyGuard.SafetyCheckResult result = guard.check("fileRead", null);
        assertTrue(result.allowed());
    }

    @Test
    void shouldAllowWhenArgumentsAndEnvironmentAreNull() {
        ToolSafetyGuard.SafetyCheckResult result = guard.check("fileRead", null, null);
        assertTrue(result.allowed());
    }

    @Test
    void shouldAllowSafeToolWithNullArguments() {
        ToolSafetyGuard.SafetyCheckResult result = guard.check("calculator", null);
        assertTrue(result.allowed());
    }

    // --- Two-arg overload tests ---

    @Test
    void twoArgOverloadShouldDelegateToThreeArg() {
        ToolSafetyGuard.SafetyCheckResult result = guard.check("volumeDelete", "{\"id\":\"123\"}");
        assertFalse(result.allowed());
        assertTrue(result.blocked());
    }

    @Test
    void twoArgOverloadShouldAllowSafeTool() {
        ToolSafetyGuard.SafetyCheckResult result = guard.check("search", "{\"query\":\"test\"}");
        assertTrue(result.allowed());
    }

    // --- Argument normalization tests (basic) ---

    @Test
    void shouldBlockObfuscatedDropTableWithHtmlEntity() {
        String obfuscated = "&#68;ROP TABLE users";
        ToolSafetyGuard.SafetyCheckResult result = guard.check("executeSql", obfuscated);
        assertTrue(result.blocked(), "Should detect HTML entity obfuscation");
    }

    @Test
    void shouldBlockObfuscatedDropTableWithJsonEscape() {
        String obfuscated = "DROP\\u0020TABLE users";
        ToolSafetyGuard.SafetyCheckResult result = guard.check("executeSql", obfuscated);
        assertTrue(result.blocked(), "Should detect JSON escape obfuscation");
    }

    @Test
    void shouldBlockObfuscatedDropTableWithUnicodeHomoglyph() {
        // Cyrillic 'о' (U+043E) instead of Latin 'o'
        String obfuscated = "DRоP TABLE users";
        ToolSafetyGuard.SafetyCheckResult result = guard.check("executeSql", obfuscated);
        assertTrue(result.blocked(), "Should detect Unicode homoglyph obfuscation");
    }

    @Test
    void shouldBlockWithExtraWhitespace() {
        String obfuscated = "DROP   TABLE   users";
        ToolSafetyGuard.SafetyCheckResult result = guard.check("executeSql", obfuscated);
        assertTrue(result.blocked(), "Should detect with extra whitespace");
    }

    @Test
    void shouldBlockWithNewlineSeparatedCommand() {
        String obfuscated = "DROP\nTABLE users";
        ToolSafetyGuard.SafetyCheckResult result = guard.check("executeSql", obfuscated);
        assertTrue(result.blocked(), "Should detect newline-separated commands");
    }

    @Test
    void shouldBlockWithTabSeparatedCommand() {
        String obfuscated = "DROP\tTABLE users";
        ToolSafetyGuard.SafetyCheckResult result = guard.check("executeSql", obfuscated);
        assertTrue(result.blocked(), "Should detect tab-separated commands");
    }
}
