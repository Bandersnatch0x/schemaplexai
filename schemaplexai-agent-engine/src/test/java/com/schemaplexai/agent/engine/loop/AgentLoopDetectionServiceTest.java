package com.schemaplexai.agent.engine.loop;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AgentLoopDetectionServiceTest {

    private AgentLoopDetectionService service;

    @BeforeEach
    void setUp() {
        service = new AgentLoopDetectionService(5, 3, 3);
    }

    @Test
    void noLoopWithFewRounds() {
        LoopDetectionResult result = service.detectLoop(1L, "hash1", Collections.emptyList());
        assertFalse(result.loopDetected());
        assertNull(result.reason());
    }

    @Test
    void detectHashLoop() {
        for (int i = 0; i < 4; i++) {
            LoopDetectionResult r = service.detectLoop(1L, "same-hash", Collections.emptyList());
            if (i < 4) {
                assertFalse(r.loopDetected(), "Round " + i + " should not detect loop");
            }
        }
        LoopDetectionResult result = service.detectLoop(1L, "same-hash", Collections.emptyList());
        assertTrue(result.loopDetected());
        assertEquals(LoopDetectionResult.HASH_LOOP, result.reason());
    }

    @Test
    void detectToolSequenceLoop() {
        List<String> tools = Arrays.asList("toolA", "toolB");
        for (int i = 0; i < 4; i++) {
            LoopDetectionResult r = service.detectLoop(2L, "hash" + i, tools);
            assertFalse(r.loopDetected(), "Round " + i + " should not detect loop");
        }
        LoopDetectionResult result = service.detectLoop(2L, "hash4", tools);
        assertTrue(result.loopDetected());
        assertEquals(LoopDetectionResult.TOOL_SEQUENCE_LOOP, result.reason());
    }

    @Test
    void noLoopWithDifferentHashes() {
        for (int i = 0; i < 10; i++) {
            LoopDetectionResult result = service.detectLoop(3L, "hash" + i, Collections.emptyList());
            assertFalse(result.loopDetected(), "Round " + i + " should not detect loop");
        }
    }

    @Test
    void noLoopWithDifferentToolSequences() {
        for (int i = 0; i < 10; i++) {
            List<String> tools = Arrays.asList("tool" + i);
            LoopDetectionResult result = service.detectLoop(4L, "hash" + i, tools);
            assertFalse(result.loopDetected(), "Round " + i + " should not detect loop");
        }
    }

    @Test
    void hashLoopNotTriggeredBelowThreshold() {
        for (int i = 0; i < 5; i++) {
            String hash = i < 2 ? "same" : "different" + i;
            LoopDetectionResult result = service.detectLoop(5L, hash, Collections.emptyList());
            assertFalse(result.loopDetected(), "Round " + i + " should not detect loop");
        }
    }

    @Test
    void toolSequenceLoopNotTriggeredBelowThreshold() {
        List<String> tools = Arrays.asList("toolA", "toolB");
        for (int i = 0; i < 5; i++) {
            List<String> roundTools = i < 2 ? tools : Arrays.asList("tool" + i);
            LoopDetectionResult result = service.detectLoop(6L, "hash" + i, roundTools);
            assertFalse(result.loopDetected(), "Round " + i + " should not detect loop");
        }
    }

    @Test
    void windowSlidesCorrectly() {
        for (int i = 0; i < 5; i++) {
            service.detectLoop(7L, "hash" + i, Collections.emptyList());
        }
        assertFalse(service.detectLoop(7L, "same", Collections.emptyList()).loopDetected());
        for (int i = 0; i < 4; i++) {
            service.detectLoop(7L, "same", Collections.emptyList());
        }
        assertTrue(service.detectLoop(7L, "same", Collections.emptyList()).loopDetected());
    }

    @Test
    void clearRecordsRemovesHistory() {
        for (int i = 0; i < 5; i++) {
            service.detectLoop(8L, "same", Collections.emptyList());
        }
        assertTrue(service.detectLoop(8L, "same", Collections.emptyList()).loopDetected());

        service.clearRecords(8L);

        LoopDetectionResult result = service.detectLoop(8L, "same", Collections.emptyList());
        assertFalse(result.loopDetected(), "After clear, loop should not be detected");
    }

    @Test
    void nullToolNamesHandled() {
        for (int i = 0; i < 5; i++) {
            LoopDetectionResult result = service.detectLoop(9L, "hash" + i, null);
            assertFalse(result.loopDetected());
        }
    }

    @Test
    void separateExecutionsDoNotInterfere() {
        for (int i = 0; i < 5; i++) {
            service.detectLoop(10L, "same", Collections.emptyList());
            service.detectLoop(11L, "other", Collections.emptyList());
        }
        assertTrue(service.detectLoop(10L, "same", Collections.emptyList()).loopDetected());
        assertFalse(service.detectLoop(11L, "different", Collections.emptyList()).loopDetected());
    }

    @Test
    void getRecordCountReturnsCorrectValue() {
        assertEquals(0, service.getRecordCount(12L));
        service.detectLoop(12L, "hash", Collections.emptyList());
        assertEquals(1, service.getRecordCount(12L));
        service.detectLoop(12L, "hash2", Collections.emptyList());
        assertEquals(2, service.getRecordCount(12L));
    }
}
