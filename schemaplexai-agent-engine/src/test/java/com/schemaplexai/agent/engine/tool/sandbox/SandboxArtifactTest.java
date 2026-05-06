package com.schemaplexai.agent.engine.tool.sandbox;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class SandboxArtifactTest {

    @Test
    void shouldCreateArtifact() {
        SandboxArtifact artifact = new SandboxArtifact(
                Path.of("output.txt"), 100L, Instant.now(), ArtifactKind.FILE);

        assertEquals(Path.of("output.txt"), artifact.relativePath());
        assertEquals(100L, artifact.sizeBytes());
        assertEquals(ArtifactKind.FILE, artifact.kind());
    }

    @Test
    void shouldRejectNullRelativePath() {
        assertThrows(NullPointerException.class, () ->
                new SandboxArtifact(null, 100L, Instant.now(), ArtifactKind.FILE));
    }

    @Test
    void shouldRejectNullCreatedAt() {
        assertThrows(NullPointerException.class, () ->
                new SandboxArtifact(Path.of("output.txt"), 100L, null, ArtifactKind.FILE));
    }

    @Test
    void shouldRejectNullKind() {
        assertThrows(NullPointerException.class, () ->
                new SandboxArtifact(Path.of("output.txt"), 100L, Instant.now(), null));
    }

    @Test
    void shouldRejectNegativeSizeBytes() {
        assertThrows(IllegalArgumentException.class, () ->
                new SandboxArtifact(Path.of("output.txt"), -1L, Instant.now(), ArtifactKind.FILE));
    }

    @Test
    void shouldAllowZeroSizeBytes() {
        SandboxArtifact artifact = new SandboxArtifact(
                Path.of("empty.txt"), 0L, Instant.now(), ArtifactKind.FILE);

        assertEquals(0L, artifact.sizeBytes());
    }

    @Test
    void shouldSupportAllArtifactKinds() {
        Instant now = Instant.now();

        SandboxArtifact file = new SandboxArtifact(Path.of("f.txt"), 1L, now, ArtifactKind.FILE);
        SandboxArtifact log = new SandboxArtifact(Path.of("l.log"), 1L, now, ArtifactKind.LOG);
        SandboxArtifact snapshot = new SandboxArtifact(Path.of("s.snap"), 1L, now, ArtifactKind.SNAPSHOT);

        assertEquals(ArtifactKind.FILE, file.kind());
        assertEquals(ArtifactKind.LOG, log.kind());
        assertEquals(ArtifactKind.SNAPSHOT, snapshot.kind());
    }
}
