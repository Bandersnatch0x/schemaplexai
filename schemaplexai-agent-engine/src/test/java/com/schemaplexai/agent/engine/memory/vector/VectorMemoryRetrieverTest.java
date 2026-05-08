package com.schemaplexai.agent.engine.memory.vector;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VectorMemoryRetrieverTest {

    @Mock
    private VectorMemoryStore vectorMemoryStore;

    private VectorMemoryRetriever retriever;

    @BeforeEach
    void setUp() {
        retriever = new VectorMemoryRetriever(vectorMemoryStore);
    }

    @Test
    void retrieveContextFormatsFragmentsAsContext() {
        List<MemoryFragment> fragments = List.of(
                createFragment("Spring Boot uses auto-configuration", "consolidation", 0.8),
                createFragment("Java records are immutable", "conversation", 0.5)
        );
        when(vectorMemoryStore.retrieve("agent-1", "tenant-1", "spring", 5)).thenReturn(fragments);

        String context = retriever.retrieveContext("agent-1", "tenant-1", "spring", 1000);

        assertThat(context).contains("## Long-term Memory");
        assertThat(context).contains("[consolidation] Spring Boot uses auto-configuration");
        assertThat(context).contains("(importance: 0.8)");
        assertThat(context).contains("[conversation] Java records are immutable");
    }

    @Test
    void retrieveContextReturnsEmptyWhenNoFragmentsFound() {
        when(vectorMemoryStore.retrieve(anyString(), anyString(), anyString(), anyInt())).thenReturn(List.of());

        String context = retriever.retrieveContext("agent-1", "tenant-1", "nonexistent", 1000);

        assertThat(context).isEmpty();
    }

    @Test
    void retrieveContextReturnsEmptyForNullAgentId() {
        String context = retriever.retrieveContext(null, "tenant-1", "query", 1000);

        assertThat(context).isEmpty();
        verifyNoInteractions(vectorMemoryStore);
    }

    @Test
    void retrieveContextReturnsEmptyForBlankTenantId() {
        String context = retriever.retrieveContext("agent-1", "  ", "query", 1000);

        assertThat(context).isEmpty();
        verifyNoInteractions(vectorMemoryStore);
    }

    @Test
    void retrieveContextReturnsEmptyForBlankQuery() {
        String context = retriever.retrieveContext("agent-1", "tenant-1", "", 1000);

        assertThat(context).isEmpty();
        verifyNoInteractions(vectorMemoryStore);
    }

    @Test
    void retrieveContextTruncatesToMaxTokens() {
        // Each token ~4 chars. With maxTokens=50, maxChars=200.
        // A long fragment should be excluded when it would exceed the limit.
        String longContent = "A".repeat(300);
        List<MemoryFragment> fragments = List.of(
                createFragment("short", "test", 0.5),
                createFragment(longContent, "test", 0.5)
        );
        when(vectorMemoryStore.retrieve(anyString(), anyString(), anyString(), anyInt())).thenReturn(fragments);

        String context = retriever.retrieveContext("agent-1", "tenant-1", "query", 50);

        assertThat(context).contains("short");
        // The long content should be truncated or excluded
        assertThat(context.length()).isLessThanOrEqualTo(300);
    }

    @Test
    void retrieveContextUsesDefaultMaxTokensWhenZero() {
        when(vectorMemoryStore.retrieve("agent-1", "tenant-1", "query", 5)).thenReturn(List.of());

        retriever.retrieveContext("agent-1", "tenant-1", "query", 0);

        verify(vectorMemoryStore).retrieve("agent-1", "tenant-1", "query", 5);
    }

    @Test
    void formatAsContextHandlesNullSource() {
        MemoryFragment fragment = new MemoryFragment(
                "id", "agent-1", "tenant-1", "content text", null, 0.5, Instant.now(), Map.of()
        );

        String context = retriever.formatAsContext(List.of(fragment), 10000);

        assertThat(context).contains("content text");
        assertThat(context).doesNotContain("[null]");
    }

    @Test
    void formatAsContextSkipsImportanceWhenZero() {
        MemoryFragment fragment = new MemoryFragment(
                "id", "agent-1", "tenant-1", "content", "test", 0.0, Instant.now(), Map.of()
        );

        String context = retriever.formatAsContext(List.of(fragment), 10000);

        assertThat(context).contains("content");
        assertThat(context).doesNotContain("importance");
    }

    @Test
    void formatAsContextReturnsEmptyForEmptyFragments() {
        String context = retriever.formatAsContext(List.of(), 10000);

        assertThat(context).isEmpty();
    }

    @Test
    void retrieveContextHandlesNullFragmentList() {
        when(vectorMemoryStore.retrieve(anyString(), anyString(), anyString(), anyInt())).thenReturn(null);

        String context = retriever.retrieveContext("agent-1", "tenant-1", "query", 1000);

        assertThat(context).isEmpty();
    }

    // --- helpers ---

    private MemoryFragment createFragment(String content, String source, double importance) {
        return new MemoryFragment(
                java.util.UUID.randomUUID().toString(),
                "agent-1", "tenant-1", content, source, importance, Instant.now(), new HashMap<>()
        );
    }
}
