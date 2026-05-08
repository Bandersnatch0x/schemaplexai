package com.schemaplexai.workflow.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TopologyHasherTest {

    @Test
    void hash_null_returnsNull() {
        assertNull(TopologyHasher.hash(null));
    }

    @Test
    void hash_blank_returnsNull() {
        assertNull(TopologyHasher.hash("  "));
    }

    @Test
    void hash_validJson_returnsConsistentHash() {
        String json = "[{\"nodeId\":\"n1\",\"nodeType\":\"AI\",\"input\":{}}]";

        String hash1 = TopologyHasher.hash(json);
        String hash2 = TopologyHasher.hash(json);

        assertNotNull(hash1);
        assertEquals(hash1, hash2);
        assertEquals(64, hash1.length()); // SHA-256 hex = 64 chars
    }

    @Test
    void hash_differentWhitespace_sameHash() {
        String compact = "[{\"nodeId\":\"n1\",\"nodeType\":\"AI\",\"input\":{}}]";
        String pretty = "[\n  {\n    \"nodeId\": \"n1\",\n    \"nodeType\": \"AI\",\n    \"input\": {}\n  }\n]";

        String hash1 = TopologyHasher.hash(compact);
        String hash2 = TopologyHasher.hash(pretty);

        assertEquals(hash1, hash2, "Semantically equivalent JSON should produce the same hash");
    }

    @Test
    void hash_differentContent_differentHash() {
        String json1 = "[{\"nodeId\":\"n1\",\"nodeType\":\"AI\",\"input\":{}}]";
        String json2 = "[{\"nodeId\":\"n2\",\"nodeType\":\"HTTP\",\"input\":{}}]";

        String hash1 = TopologyHasher.hash(json1);
        String hash2 = TopologyHasher.hash(json2);

        assertNotEquals(hash1, hash2);
    }

    @Test
    void hash_preservesStringContent_withSpaces() {
        String json1 = "[{\"nodeId\":\"n1\",\"input\":{\"prompt\":\"hello world\"}}]";
        String json2 = "[{\"nodeId\":\"n1\",\"input\":{\"prompt\":\"helloworld\"}}]";

        String hash1 = TopologyHasher.hash(json1);
        String hash2 = TopologyHasher.hash(json2);

        assertNotEquals(hash1, hash2, "Spaces inside strings should be preserved");
    }

    // ------------------------------------------------------------------
    // verify
    // ------------------------------------------------------------------

    @Test
    void verify_matchingHash_doesNotThrow() {
        String json = "[{\"nodeId\":\"n1\",\"nodeType\":\"AI\",\"input\":{}}]";
        String hash = TopologyHasher.hash(json);

        assertDoesNotThrow(() -> TopologyHasher.verify(hash, json));
    }

    @Test
    void verify_nullExpectedAndCurrent_doesNotThrow() {
        assertDoesNotThrow(() -> TopologyHasher.verify(null, null));
    }

    @Test
    void verify_nullExpectedAndNonNullCurrent_throwsMismatch() {
        String json = "[{\"nodeId\":\"n1\"}]";

        assertThrows(TopologyMismatchException.class,
                () -> TopologyHasher.verify(null, json));
    }

    @Test
    void verify_nonNullExpectedAndNullCurrent_throwsMismatch() {
        assertThrows(TopologyMismatchException.class,
                () -> TopologyHasher.verify("abc123", null));
    }

    @Test
    void verify_mismatchedHash_throwsMismatch() {
        String json1 = "[{\"nodeId\":\"n1\"}]";
        String json2 = "[{\"nodeId\":\"n2\"}]";
        String hash1 = TopologyHasher.hash(json1);

        TopologyMismatchException ex = assertThrows(TopologyMismatchException.class,
                () -> TopologyHasher.verify(hash1, json2));

        assertTrue(ex.getMessage().contains("Topology mismatch"));
        assertTrue(ex.getMessage().contains("checkpoint hash="));
        assertTrue(ex.getMessage().contains("current hash="));
    }

    // ------------------------------------------------------------------
    // normalize
    // ------------------------------------------------------------------

    @Test
    void normalize_stripsWhitespaceOutsideStrings() {
        String input = "{ \"key\" : \"value\" }";
        String normalized = TopologyHasher.normalize(input);

        assertEquals("{\"key\":\"value\"}", normalized);
    }

    @Test
    void normalize_preservesWhitespaceInsideStrings() {
        String input = "{\"key\":\"hello world\"}";
        String normalized = TopologyHasher.normalize(input);

        assertEquals("{\"key\":\"hello world\"}", normalized);
    }

    @Test
    void normalize_handlesEscapedQuotes() {
        String input = "{\"key\":\"hello \\\"world\\\"\"}";
        String normalized = TopologyHasher.normalize(input);

        assertEquals("{\"key\":\"hello \\\"world\\\"\"}", normalized);
    }
}
