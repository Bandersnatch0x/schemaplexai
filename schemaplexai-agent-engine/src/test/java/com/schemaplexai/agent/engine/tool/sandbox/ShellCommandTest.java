package com.schemaplexai.agent.engine.tool.sandbox;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ShellCommandTest {

    @Test
    void shouldRejectEmptyArgv() {
        assertThrows(IllegalArgumentException.class, () ->
                new ShellCommand(List.of(), Map.of(), Duration.ofSeconds(5), null));
    }

    @Test
    void shouldRejectNullArgv() {
        assertThrows(NullPointerException.class, () ->
                new ShellCommand(null, Map.of(), Duration.ofSeconds(5), null));
    }

    @Test
    void shouldNormalizeNullEnv() {
        ShellCommand cmd = new ShellCommand(List.of("echo"), null, null, null);
        assertNotNull(cmd.env());
        assertTrue(cmd.env().isEmpty());
    }

    @Test
    void shouldStoreWorkingDir() {
        Path wd = Path.of("/tmp/sample");
        ShellCommand cmd = new ShellCommand(List.of("ls"), Map.of(), Duration.ofSeconds(1), wd);
        assertEquals(wd, cmd.workingDir());
    }

    @Test
    void shouldRejectNegativeTimeout() {
        assertThrows(IllegalArgumentException.class, () ->
                new ShellCommand(List.of("echo"), Map.of(), Duration.ofSeconds(-1), null));
    }
}
