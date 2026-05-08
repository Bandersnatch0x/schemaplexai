package com.schemaplexai.agent.engine.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HashUtilsTest {

    @Test
    void sha256_producesConsistentHexHash() {
        String input = "test snapshot json";
        String hash1 = HashUtils.sha256(input);
        String hash2 = HashUtils.sha256(input);

        assertEquals(64, hash1.length());
        assertTrue(hash1.matches("^[0-9a-f]{64}$"));
        assertEquals(hash1, hash2);
    }

    @Test
    void sha256_differentInputsProduceDifferentHashes() {
        String hash1 = HashUtils.sha256("input A");
        String hash2 = HashUtils.sha256("input B");

        assertNotEquals(hash1, hash2);
    }

    @Test
    void constantTimeEquals_sameStrings_returnsTrue() {
        assertTrue(HashUtils.constantTimeEquals("abc", "abc"));
    }

    @Test
    void constantTimeEquals_differentStrings_returnsFalse() {
        assertFalse(HashUtils.constantTimeEquals("abc", "def"));
    }

    @Test
    void constantTimeEquals_sameHashStrings_returnsTrue() {
        String hash = HashUtils.sha256("data");
        assertTrue(HashUtils.constantTimeEquals(hash, HashUtils.sha256("data")));
    }

    @Test
    void constantTimeEquals_nullHandling() {
        assertTrue(HashUtils.constantTimeEquals(null, null));
        assertFalse(HashUtils.constantTimeEquals(null, "a"));
        assertFalse(HashUtils.constantTimeEquals("a", null));
    }
}
