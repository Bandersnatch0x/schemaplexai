package com.schemaplexai.agent.engine.tool.sandbox.provider;

import com.schemaplexai.agent.engine.tool.ToolErrorCategory;
import com.schemaplexai.agent.engine.tool.sandbox.SandboxException;
import com.schemaplexai.agent.engine.tool.sandbox.SandboxProvider;
import com.schemaplexai.agent.engine.tool.sandbox.SandboxSession;
import com.schemaplexai.agent.engine.tool.sandbox.SandboxSessionConfig;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Tests for {@link ContainerSandboxProvider} and {@link ContainerSandboxSession}.
 *
 * <p>These tests are skipped if the container sandbox provider is not available
 * (Docker Java API missing or {@code sandbox.provider != container}).
 */
class ContainerSandboxTest {

    private final SandboxProvider provider = resolveProvider();

    private static SandboxProvider resolveProvider() {
        try {
            Class<?> clazz = Class.forName(
                    "com.schemaplexai.agent.engine.tool.sandbox.provider.ContainerSandboxProvider");
            return (SandboxProvider) clazz.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException | ClassCastException e) {
            return null;
        }
    }

    @Test
    void shouldExposeProviderId() {
        assumeTrue(provider != null, "ContainerSandboxProvider not enabled");
        assertEquals("container", provider.providerId());
    }

    @Test
    void shouldRejectNullConfig() {
        assumeTrue(provider != null, "ContainerSandboxProvider not enabled");
        SandboxException ex = assertThrows(SandboxException.class, () -> provider.create(null));
        assertEquals(ToolErrorCategory.SANDBOX_ERROR, ex.getCategory());
        assertTrue(ex.getMessage().contains("config required"));
    }

    @Test
    void shouldCreateSessionWithValidConfig() throws Exception {
        assumeTrue(provider != null, "ContainerSandboxProvider not enabled");
        try (SandboxSession session = provider.create(SandboxSessionConfig.defaults())) {
            assertNotNull(session.sessionId());
            assertNotNull(session.workspaceRoot());
        }
    }

    @Test
    void shouldCreateMultipleSessionsIndependently() throws Exception {
        assumeTrue(provider != null, "ContainerSandboxProvider not enabled");
        try (SandboxSession session1 = provider.create(SandboxSessionConfig.defaults())) {
            try (SandboxSession session2 = provider.create(SandboxSessionConfig.defaults())) {
                assertNotEquals(session1.sessionId(), session2.sessionId());
                assertNotEquals(session1.workspaceRoot(), session2.workspaceRoot());
            }
        }
    }

    @Test
    void shouldScopeChildSession() throws Exception {
        assumeTrue(provider != null, "ContainerSandboxProvider not enabled");
        try (SandboxSession parent = provider.create(SandboxSessionConfig.defaults())) {
            SandboxSessionConfig childConfig = new SandboxSessionConfig(
                    Duration.ofSeconds(10),
                    256L,
                    15_000L,
                    null,
                    Map.of("CHILD_VAR", "child"),
                    com.schemaplexai.agent.engine.tool.sandbox.NetworkPolicy.NONE,
                    List.of()
            );
            try (SandboxSession child = provider.scope(parent, childConfig)) {
                assertNotNull(child.sessionId());
                assertNotEquals(parent.sessionId(), child.sessionId());
            }
        }
    }
}
