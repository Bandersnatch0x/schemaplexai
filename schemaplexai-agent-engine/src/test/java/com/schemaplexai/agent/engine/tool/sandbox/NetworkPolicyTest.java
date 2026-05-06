package com.schemaplexai.agent.engine.tool.sandbox;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NetworkPolicyTest {

    @Test
    void shouldContainAllPolicies() {
        assertNotNull(NetworkPolicy.NONE);
        assertNotNull(NetworkPolicy.LOOPBACK);
        assertNotNull(NetworkPolicy.OPEN);
    }

    @Test
    void shouldHaveThreeValues() {
        assertEquals(3, NetworkPolicy.values().length);
    }

    @Test
    void shouldReturnCorrectEnumByName() {
        assertEquals(NetworkPolicy.NONE, NetworkPolicy.valueOf("NONE"));
        assertEquals(NetworkPolicy.LOOPBACK, NetworkPolicy.valueOf("LOOPBACK"));
        assertEquals(NetworkPolicy.OPEN, NetworkPolicy.valueOf("OPEN"));
    }
}
