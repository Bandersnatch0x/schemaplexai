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
        assertEquals(ToolErrorCategory.UNAUTHORIZED_SCOPE, result.errorCategory());
        assertTrue(result.reason().contains("irreversible"));
    }

    @Test
    void shouldBlockDropDatabaseOperations() {
        ToolSafetyGuard.SafetyCheckResult result = guard.check("executeSql", "DROP TABLE users");
        assertFalse(result.allowed());
        assertTrue(result.blocked());
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
    void shouldBlockWhenCredentialsMismatch() {
        ToolSafetyGuard.SafetyCheckResult result = guard.check("deploy", "{\"env\":\"production\",\"token\":\"prod-token\"}", "staging");
        assertFalse(result.allowed());
        assertTrue(result.blocked());
        assertEquals(ToolErrorCategory.UNAUTHORIZED_SCOPE, result.errorCategory());
    }
}
