package com.schemaplexai.agent.engine.tool.sandbox;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MountSpecTest {

    @Test
    void shouldCreateMountSpec() {
        MountSpec spec = new MountSpec(Path.of("/host"), Path.of("/mnt"), true);

        assertEquals(Path.of("/host"), spec.hostPath());
        assertEquals(Path.of("/mnt"), spec.containerPath());
        assertTrue(spec.readOnly());
    }

    @Test
    void shouldCreateWritableMountSpec() {
        MountSpec spec = new MountSpec(Path.of("/host"), Path.of("/mnt"), false);

        assertFalse(spec.readOnly());
    }

    @Test
    void shouldRejectNullHostPath() {
        assertThrows(NullPointerException.class, () ->
                new MountSpec(null, Path.of("/mnt"), true));
    }

    @Test
    void shouldRejectNullContainerPath() {
        assertThrows(NullPointerException.class, () ->
                new MountSpec(Path.of("/host"), null, true));
    }
}
