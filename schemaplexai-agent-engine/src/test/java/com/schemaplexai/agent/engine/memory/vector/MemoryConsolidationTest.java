package com.schemaplexai.agent.engine.memory.vector;

import com.schemaplexai.agent.engine.memory.CompositeChatMemoryStore;
import com.schemaplexai.agent.engine.model.LlmMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemoryConsolidationTest {

    @Mock
    private VectorMemoryStore vectorMemoryStore;

    @Mock
    private CompositeChatMemoryStore chatMemoryStore;

    private MemoryConsolidationService consolidationService;

    @BeforeEach
    void setUp() {
        consolidationService = new MemoryConsolidationService(vectorMemoryStore, chatMemoryStore);
    }

    @Test
    void consolidateStoresFragmentFromChatMessages() {
        List<LlmMessage> messages = List.of(
                new LlmMessage("user", "How do I configure Spring Boot?"),
                new LlmMessage("assistant", "You can use application.yml"),
                new LlmMessage("user", "Thanks!")
        );
        when(chatMemoryStore.loadMessages("conv-1")).thenReturn(messages);

        consolidationService.consolidate("agent-1", "tenant-1", "conv-1");

        ArgumentCaptor<MemoryFragment> captor = ArgumentCaptor.forClass(MemoryFragment.class);
        verify(vectorMemoryStore).store(captor.capture());
        MemoryFragment stored = captor.getValue();
        assertThat(stored.agentId()).isEqualTo("agent-1");
        assertThat(stored.tenantId()).isEqualTo("tenant-1");
        assertThat(stored.source()).isEqualTo("consolidation");
        assertThat(stored.content()).contains("How do I configure Spring Boot?");
        assertThat(stored.content()).contains("application.yml");
    }

    @Test
    void consolidateSkipsWhenMessagesEmpty() {
        when(chatMemoryStore.loadMessages("conv-1")).thenReturn(List.of());

        consolidationService.consolidate("agent-1", "tenant-1", "conv-1");

        verify(vectorMemoryStore, never()).store(any());
    }

    @Test
    void consolidateSkipsWhenMessagesNull() {
        when(chatMemoryStore.loadMessages("conv-1")).thenReturn(null);

        consolidationService.consolidate("agent-1", "tenant-1", "conv-1");

        verify(vectorMemoryStore, never()).store(any());
    }

    @Test
    void consolidateSkipsWhenAgentIdBlank() {
        consolidationService.consolidate("", "tenant-1", "conv-1");

        verify(chatMemoryStore, never()).loadMessages(any());
        verify(vectorMemoryStore, never()).store(any());
    }

    @Test
    void consolidateSkipsWhenTenantIdBlank() {
        consolidationService.consolidate("agent-1", "", "conv-1");

        verify(vectorMemoryStore, never()).store(any());
    }

    @Test
    void consolidateSkipsWhenConversationIdBlank() {
        consolidationService.consolidate("agent-1", "tenant-1", "");

        verify(vectorMemoryStore, never()).store(any());
    }

    @Test
    void calculateImportanceWeightedByRole() {
        List<LlmMessage> messages = List.of(
                new LlmMessage("tool", "result data"),
                new LlmMessage("assistant", "reasoning"),
                new LlmMessage("user", "question")
        );

        double importance = consolidationService.calculateImportance(messages);

        // tool=1.0, assistant=0.7, user=0.4 -> (1.0+0.7+0.4)/3 = 0.7
        assertThat(importance).isCloseTo(0.7, within(0.01));
    }

    @Test
    void calculateImportanceHighForToolMessages() {
        List<LlmMessage> messages = List.of(
                new LlmMessage("tool", "data"),
                new LlmMessage("tool", "more data")
        );

        double importance = consolidationService.calculateImportance(messages);

        assertThat(importance).isEqualTo(1.0);
    }

    @Test
    void calculateImportanceLowForUserOnlyMessages() {
        List<LlmMessage> messages = List.of(
                new LlmMessage("user", "hello"),
                new LlmMessage("user", "hi")
        );

        double importance = consolidationService.calculateImportance(messages);

        assertThat(importance).isEqualTo(0.4);
    }

    @Test
    void calculateImportanceReturnsHalfForEmptyMessages() {
        assertThat(consolidationService.calculateImportance(List.of())).isEqualTo(0.0);
        assertThat(consolidationService.calculateImportance(null)).isEqualTo(0.0);
    }

    @Test
    void buildSummaryIncludesAllMessages() {
        List<LlmMessage> messages = List.of(
                new LlmMessage("user", "What is Java?"),
                new LlmMessage("assistant", "Java is a programming language.")
        );

        String summary = consolidationService.buildSummary(messages);

        assertThat(summary).contains("2 messages");
        assertThat(summary).contains("[user]: What is Java?");
        assertThat(summary).contains("[assistant]: Java is a programming language.");
    }

    @Test
    void buildSummaryTruncatesLongMessages() {
        String longContent = "A".repeat(600);
        List<LlmMessage> messages = List.of(
                new LlmMessage("user", longContent)
        );

        String summary = consolidationService.buildSummary(messages);

        assertThat(summary).contains("...");
        assertThat(summary).doesNotContain(longContent);
    }

    @Test
    void buildSummaryReturnsEmptyForNullOrEmpty() {
        assertThat(consolidationService.buildSummary(List.of())).isEmpty();
        assertThat(consolidationService.buildSummary(null)).isEmpty();
    }

    @Test
    void consolidateIncludesMetadataWithConversationId() {
        List<LlmMessage> messages = List.of(
                new LlmMessage("user", "test message")
        );
        when(chatMemoryStore.loadMessages("conv-42")).thenReturn(messages);

        consolidationService.consolidate("agent-1", "tenant-1", "conv-42");

        ArgumentCaptor<MemoryFragment> captor = ArgumentCaptor.forClass(MemoryFragment.class);
        verify(vectorMemoryStore).store(captor.capture());
        MemoryFragment stored = captor.getValue();
        assertThat(stored.metadata()).containsEntry("conversationId", "conv-42");
        assertThat(stored.metadata()).containsEntry("messageCount", 1);
    }
}
