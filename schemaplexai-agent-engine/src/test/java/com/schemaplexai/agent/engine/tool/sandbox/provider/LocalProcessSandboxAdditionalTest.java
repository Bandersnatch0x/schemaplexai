package com.schemaplexai.agent.engine.tool.sandbox.provider;

import com.schemaplexai.agent.engine.tool.ToolErrorCategory;
import com.schemaplexai.agent.engine.tool.sandbox.SandboxException;
import com.schemaplexai.agent.engine.tool.sandbox.SandboxSession;
import com.schemaplexai.agent.engine.tool.sandbox.SandboxSessionConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LocalProcessSandboxAdditionalTest {

    private final LocalProcessSandbox sandbox = new LocalProcessSandbox();

    @Test
    void shouldExposeProviderId() {
        assertEquals("local", sandbox.providerId());
    }

    @Test
    void shouldRejectNullConfig() {
        SandboxException ex = assertThrows(SandboxException.class, () -> sandbox.create(null));
        assertEquals(ToolErrorCategory.SANDBOX_ERROR, ex.getCategory());
        assertTrue(ex.getMessage().contains("config required"));
    }

    @Test
    void shouldCreateSessionWithValidConfig() throws Exception {
        try (SandboxSession session = sandbox.create(SandboxSessionConfig.defaults())) {
            assertNotNull(session.sessionId());
            assertNotNull(session.workspaceRoot());
        }
    }

    @Test
    void shouldCreateMultipleSessionsIndependently() throws Exception {
        try (SandboxSession session1 = sandbox.create(SandboxSessionConfig.defaults())) {
            try (SandboxSession session2 = sandbox.create(SandboxSessionConfig.defaults())) {
                assertNotEquals(session1.sessionId(), session2.sessionId());
                assertNotEquals(session1.workspaceRoot(), session2.workspaceRoot());
            }
        }
    }
}
