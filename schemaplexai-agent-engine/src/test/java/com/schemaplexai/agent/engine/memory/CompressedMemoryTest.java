package com.schemaplexai.agent.engine.memory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CompressedMemory")
class CompressedMemoryTest {

    @Test
    @DisplayName("should create via factory with current timestamp")
    void shouldCreateViaFactory() {
        Instant before = Instant.now();
        CompressedMemory memory = CompressedMemory.of("summary", 5, 100, List.of());
        Instant after = Instant.now();

        assertThat(memory.getSummary()).isEqualTo("summary");
        assertThat(memory.getOriginalMessageCount()).isEqualTo(5);
        assertThat(memory.getCompressedTokenCount()).isEqualTo(100);
        assertThat(memory.getRetainedMessages()).isEmpty();
        assertThat(memory.getCompressedAt()).isBetween(before, after);
    }

    @Test
    @DisplayName("should create via all-args constructor")
    void shouldCreateViaAllArgsConstructor() {
        Instant now = Instant.parse("2024-01-01T00:00:00Z");
        List<ChatMessage> retained = List.of(ChatMessage.user("kept"));

        CompressedMemory memory = new CompressedMemory("summary", 3, 50, now, retained);

        assertThat(memory.getSummary()).isEqualTo("summary");
        assertThat(memory.getOriginalMessageCount()).isEqualTo(3);
        assertThat(memory.getCompressedTokenCount()).isEqualTo(50);
        assertThat(memory.getCompressedAt()).isEqualTo(now);
        assertThat(memory.getRetainedMessages()).hasSize(1);
    }

    @Test
    @DisplayName("should support no-args constructor for frameworks")
    void shouldSupportNoArgsConstructor() {
        CompressedMemory memory = new CompressedMemory();

        assertThat(memory.getSummary()).isNull();
        assertThat(memory.getOriginalMessageCount()).isEqualTo(0);
        assertThat(memory.getCompressedTokenCount()).isEqualTo(0);
        assertThat(memory.getCompressedAt()).isNull();
        assertThat(memory.getRetainedMessages()).isNull();
    }

    @Test
    @DisplayName("should return true for hasSummary when summary is present")
    void shouldReturnTrueForHasSummary() {
        CompressedMemory memory = CompressedMemory.of("valid summary", 1, 10, List.of());

        assertThat(memory.hasSummary()).isTrue();
    }

    @Test
    @DisplayName("should return false for hasSummary when summary is null")
    void shouldReturnFalseForNullSummary() {
        CompressedMemory memory = new CompressedMemory(null, 1, 10, Instant.now(), List.of());

        assertThat(memory.hasSummary()).isFalse();
    }

    @Test
    @DisplayName("should return false for hasSummary when summary is blank")
    void shouldReturnFalseForBlankSummary() {
        CompressedMemory memory = CompressedMemory.of("   ", 1, 10, List.of());

        assertThat(memory.hasSummary()).isFalse();
    }

    @Test
    @DisplayName("should return false for hasSummary when summary is empty")
    void shouldReturnFalseForEmptySummary() {
        CompressedMemory memory = CompressedMemory.of("", 1, 10, List.of());

        assertThat(memory.hasSummary()).isFalse();
    }

    @Test
    @DisplayName("should support setters")
    void shouldSupportSetters() {
        CompressedMemory memory = new CompressedMemory();
        memory.setSummary("new summary");
        memory.setOriginalMessageCount(10);
        memory.setCompressedTokenCount(200);
        memory.setCompressedAt(Instant.now());
        memory.setRetainedMessages(List.of(ChatMessage.assistant("retained")));

        assertThat(memory.getSummary()).isEqualTo("new summary");
        assertThat(memory.getOriginalMessageCount()).isEqualTo(10);
        assertThat(memory.getCompressedTokenCount()).isEqualTo(200);
        assertThat(memory.getRetainedMessages()).hasSize(1);
    }

    @Test
    @DisplayName("should consider two instances equal when fields match")
    void shouldBeEqualWhenFieldsMatch() {
        Instant now = Instant.now();
        CompressedMemory m1 = new CompressedMemory("s", 1, 10, now, List.of());
        CompressedMemory m2 = new CompressedMemory("s", 1, 10, now, List.of());

        assertThat(m1).isEqualTo(m2);
        assertThat(m1.hashCode()).isEqualTo(m2.hashCode());
    }

    @Test
    @DisplayName("should not be equal when fields differ")
    void shouldNotBeEqualWhenFieldsDiffer() {
        CompressedMemory m1 = new CompressedMemory("s1", 1, 10, Instant.now(), List.of());
        CompressedMemory m2 = new CompressedMemory("s2", 1, 10, Instant.now(), List.of());

        assertThat(m1).isNotEqualTo(m2);
    }
}
