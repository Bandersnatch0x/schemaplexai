package com.schemaplexai.context.service.impl;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

class NoOpFileScanServiceTest {

    private final NoOpFileScanService service = new NoOpFileScanService();

    @Test
    void scan_doesNotThrow() {
        InputStream input = new ByteArrayInputStream("test content".getBytes());
        // No-op scan should complete without exception
        service.scan(input, "test.txt");
    }

    @Test
    void scan_acceptsNullInputStream() {
        // No-op implementation should handle any input gracefully
        service.scan(null, "null-file.txt");
    }

    @Test
    void isHealthy_alwaysReturnsTrue() {
        assertThat(service.isHealthy()).isTrue();
    }
}
