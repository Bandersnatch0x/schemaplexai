package com.schemaplexai.agent.engine.tool;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ToolSafetyGuardTest {

    private final ToolSafetyGuard guard = new ToolSafetyGuard();

    @Test
    void shouldBlockIrreversibleDeleteOperations() {
        ToolSafetyGuard.SafetyCheckResult result = guard.check("volumeDelete", "{\"id\":\"vol-123\"}");
        assertFalse(result.allowed());
        assertTrue(result.blocked());
        assertEquals(ToolErrorCategory.IRREVERSIBLE_OPERATION, result.errorCategory());
    }

    @Test
    void shouldBlockDropDatabaseOperations() {
        ToolSafetyGuard.SafetyCheckResult result = guard.check("executeSql", "DROP TABLE users");
        assertFalse(result.allowed());
        assertTrue(result.blocked());
        assertEquals(ToolErrorCategory.IRREVERSIBLE_OPERATION, result.errorCategory());
    }

    @Test
    void shouldAllowReadOperations() {
        ToolSafetyGuard.SafetyCheckResult result = guard.check("fileRead", "{\"path\":\"/tmp/log.txt\"}");
        assertTrue(result.allowed());
        assertFalse(result.blocked());
        assertNull(result.errorCategory());
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
    void shouldBlockWhenCredentialsMismatch() {
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
    void shouldAllowWhenArgumentsAreNull() {
        ToolSafetyGuard.SafetyCheckResult result = guard.check("fileRead", null);
        assertTrue(result.allowed());
    }
}
