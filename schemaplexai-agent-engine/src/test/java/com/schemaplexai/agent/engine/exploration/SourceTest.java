package com.schemaplexai.agent.engine.exploration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Source")
class SourceTest {

    @Test
    @DisplayName("should store all fields correctly")
    void shouldStoreAllFields() {
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        Source source = new Source("https://example.com", "Example", "content", 0.95, now);

        assertEquals("https://example.com", source.url());
        assertEquals("Example", source.title());
        assertEquals("content", source.content());
        assertEquals(0.95, source.relevanceScore(), 0.001);
        assertEquals(now, source.timestamp());
    }

    @Test
    @DisplayName("should support equality based on field values")
    void shouldSupportEquality() {
        Instant now = Instant.now();
        Source s1 = new Source("https://a.com", "A", "c", 0.5, now);
        Source s2 = new Source("https://a.com", "A", "c", 0.5, now);

        assertEquals(s1, s2);
        assertEquals(s1.hashCode(), s2.hashCode());
    }
}
