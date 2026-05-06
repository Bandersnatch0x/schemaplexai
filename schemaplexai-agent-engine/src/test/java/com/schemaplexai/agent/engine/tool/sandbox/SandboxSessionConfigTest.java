package com.schemaplexai.agent.engine.tool.sandbox;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SandboxSessionConfigTest {

    @Test
    void defaultsShouldUseSafeValues() {
        SandboxSessionConfig config = SandboxSessionConfig.defaults();

        assertEquals(Duration.ofSeconds(30), config.timeout());
        assertEquals(512L, config.memoryLimitMb());
        assertEquals(30_000L, config.cpuLimitMillis());
        assertNull(config.workspaceImage());
        assertNotNull(config.envVars());
        assertTrue(config.envVars().isEmpty());
        assertEquals(NetworkPolicy.NONE, config.networkPolicy());
        assertNotNull(config.mountPaths());
        assertTrue(config.mountPaths().isEmpty());
    }

    @Test
    void shouldAcceptCustomValues() {
        SandboxSessionConfig config = new SandboxSessionConfig(
                Duration.ofMinutes(2),
                1024L,
                60_000L,
                "node:20",
                Map.of("FOO", "bar"),
                NetworkPolicy.LOOPBACK,
                List.of(new MountSpec(Path.of("/host"), Path.of("/mnt"), true))
        );

        assertEquals(Duration.ofMinutes(2), config.timeout());
        assertEquals(1024L, config.memoryLimitMb());
        assertEquals("node:20", config.workspaceImage());
        assertEquals("bar", config.envVars().get("FOO"));
        assertEquals(NetworkPolicy.LOOPBACK, config.networkPolicy());
        assertEquals(1, config.mountPaths().size());
    }

    @Test
    void shouldRejectNegativeTimeout() {
        assertThrows(IllegalArgumentException.class, () -> new SandboxSessionConfig(
                Duration.ofSeconds(-1),
                512L,
                30_000L,
                null,
                Map.of(),
                NetworkPolicy.NONE,
                List.of()
        ));
    }

    @Test
    void shouldRejectNullTimeout() {
        assertThrows(NullPointerException.class, () -> new SandboxSessionConfig(
                null,
                512L,
                30_000L,
                null,
                Map.of(),
                NetworkPolicy.NONE,
                List.of()
        ));
    }

    @Test
    void shouldNormalizeNullMapsAndLists() {
        SandboxSessionConfig config = new SandboxSessionConfig(
                Duration.ofSeconds(30),
                512L,
                30_000L,
                null,
                null,
                null,
                null
        );
        assertNotNull(config.envVars());
        assertNotNull(config.mountPaths());
        assertEquals(NetworkPolicy.NONE, config.networkPolicy());
    }
}
