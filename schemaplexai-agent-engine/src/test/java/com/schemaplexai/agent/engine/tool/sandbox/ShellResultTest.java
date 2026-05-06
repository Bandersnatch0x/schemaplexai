package com.schemaplexai.agent.engine.tool.sandbox;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class ShellResultTest {

    @Test
    void shouldCreateSuccessfulResult() {
        ShellResult result = new ShellResult(0, "output", "", Duration.ofSeconds(1), false);

        assertTrue(result.isSuccess());
        assertEquals(0, result.exitCode());
        assertEquals("output", result.stdout());
        assertEquals("", result.stderr());
        assertEquals(Duration.ofSeconds(1), result.elapsed());
        assertFalse(result.timedOut());
    }

    @Test
    void shouldCreateFailedResult() {
        ShellResult result = new ShellResult(1, "", "error", Duration.ofMillis(500), false);

        assertFalse(result.isSuccess());
        assertEquals(1, result.exitCode());
        assertEquals("error", result.stderr());
    }

    @Test
    void shouldCreateTimedOutResult() {
        ShellResult result = new ShellResult(-1, "partial", "", Duration.ofSeconds(2), true);

        assertFalse(result.isSuccess());
        assertTrue(result.timedOut());
        assertEquals(-1, result.exitCode());
    }

    @Test
    void shouldNormalizeNullStdout() {
        ShellResult result = new ShellResult(0, null, "", Duration.ZERO, false);

        assertEquals("", result.stdout());
    }

    @Test
    void shouldNormalizeNullStderr() {
        ShellResult result = new ShellResult(0, "", null, Duration.ZERO, false);

        assertEquals("", result.stderr());
    }

    @Test
    void shouldNormalizeNullElapsed() {
        ShellResult result = new ShellResult(0, "", "", null, false);

        assertEquals(Duration.ZERO, result.elapsed());
    }

    @Test
    void shouldPreserveNonNullValues() {
        ShellResult result = new ShellResult(42, "stdout", "stderr", Duration.ofMillis(100), true);

        assertEquals(42, result.exitCode());
        assertEquals("stdout", result.stdout());
        assertEquals("stderr", result.stderr());
        assertEquals(Duration.ofMillis(100), result.elapsed());
        assertTrue(result.timedOut());
    }
}
