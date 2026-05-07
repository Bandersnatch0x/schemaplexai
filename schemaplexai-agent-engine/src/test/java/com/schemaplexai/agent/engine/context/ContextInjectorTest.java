package com.schemaplexai.agent.engine.context;

import com.schemaplexai.agent.engine.model.LlmMessage;
import com.schemaplexai.agent.engine.rag.MilvusIsolationService;
import com.schemaplexai.agent.engine.rag.SearchResult;
import com.schemaplexai.common.context.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContextInjectorTest {

    @Mock
    private MilvusIsolationService ragService;

    @Mock
    private EmbeddingService embeddingService;

    private ContextInjector contextInjector;

    @BeforeEach
    void setUp() {
        contextInjector = new ContextInjector(ragService, embeddingService);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void injectShouldSkipWhenMessagesIsNull() {
        TenantContextHolder.setTenantId("tenant-1");
        contextInjector.inject(null, 1L);
        verifyNoInteractions(ragService, embeddingService);
    }

    @Test
    void injectShouldSkipWhenMessagesIsEmpty() {
        TenantContextHolder.setTenantId("tenant-1");
        contextInjector.inject(new ArrayList<>(), 1L);
        verifyNoInteractions(ragService, embeddingService);
    }

    @Test
    void injectShouldSkipWhenNoUserMessageFound() {
        TenantContextHolder.setTenantId("tenant-1");
        List<LlmMessage> messages = List.of(
                new LlmMessage("system", "You are a helper"),
                new LlmMessage("assistant", "Hello")
        );
        contextInjector.inject(new ArrayList<>(messages), 1L);
        verifyNoInteractions(ragService, embeddingService);
    }

    @Test
    void injectShouldSkipWhenTenantIdIsNull() {
        List<LlmMessage> messages = List.of(new LlmMessage("user", "What is RAG?"));
        contextInjector.inject(new ArrayList<>(messages), 1L);
        verifyNoInteractions(ragService, embeddingService);
    }

    @Test
    void injectShouldSkipWhenRagServicesAreNull() {
        TenantContextHolder.setTenantId("tenant-1");
        ContextInjector injectorWithoutRag = new ContextInjector();
        List<LlmMessage> messages = new ArrayList<>(List.of(new LlmMessage("user", "What is RAG?")));

        injectorWithoutRag.inject(messages, 1L);

        assertEquals(1, messages.size());
        assertEquals("user", messages.get(0).getRole());
    }

    @Test
    void injectShouldInsertRagContextSystemMessage() throws Exception {
        TenantContextHolder.setTenantId("tenant-1");
        String userPrompt = "What is RAG?";
        List<LlmMessage> messages = new ArrayList<>(List.of(
                new LlmMessage("system", "You are a helper"),
                new LlmMessage("user", userPrompt)
        ));

        float[] embedding = new float[]{0.1f, 0.2f, 0.3f};
        when(embeddingService.embed(userPrompt)).thenReturn(embedding);
        when(ragService.searchWithIsolation("tenant-1", null, embedding))
                .thenReturn(List.of(
                        new SearchResult("RAG stands for Retrieval-Augmented Generation.", "docs/rag.md", 0.95),
                        new SearchResult("It combines retrieval with LLM generation.", "docs/rag.md", 0.88)
                ));

        contextInjector.inject(messages, 1L);

        assertEquals(3, messages.size());
        assertEquals("system", messages.get(0).getRole());
        assertEquals("You are a helper", messages.get(0).getContent());
        assertEquals("system", messages.get(1).getRole());
        assertTrue(messages.get(1).getContent().contains("Context from knowledge base:"));
        assertTrue(messages.get(1).getContent().contains("RAG stands for Retrieval-Augmented Generation."));
        assertEquals("user", messages.get(2).getRole());
        assertEquals(userPrompt, messages.get(2).getContent());
    }

    @Test
    void injectShouldProceedOnRagFailure() throws Exception {
        TenantContextHolder.setTenantId("tenant-1");
        String userPrompt = "What is RAG?";
        List<LlmMessage> messages = new ArrayList<>(List.of(new LlmMessage("user", userPrompt)));

        when(embeddingService.embed(userPrompt)).thenThrow(new RuntimeException("Embedding service down"));

        assertDoesNotThrow(() -> contextInjector.inject(messages, 1L));

        assertEquals(1, messages.size());
        assertEquals("user", messages.get(0).getRole());
        assertEquals(userPrompt, messages.get(0).getContent());
    }

    @Test
    void injectShouldUseLatestUserMessageWhenMultipleExist() throws Exception {
        TenantContextHolder.setTenantId("tenant-1");
        List<LlmMessage> messages = new ArrayList<>(List.of(
                new LlmMessage("user", "First question"),
                new LlmMessage("assistant", "First answer"),
                new LlmMessage("user", "Follow-up question")
        ));

        float[] embedding = new float[]{0.1f, 0.2f};
        when(embeddingService.embed("Follow-up question")).thenReturn(embedding);
        when(ragService.searchWithIsolation("tenant-1", null, embedding))
                .thenReturn(List.of(new SearchResult("Follow-up context", "docs/follow-up.md", 0.92)));

        contextInjector.inject(messages, 1L);

        assertEquals(4, messages.size());
        assertEquals("user", messages.get(0).getRole());
        assertEquals("First question", messages.get(0).getContent());
        assertEquals("assistant", messages.get(1).getRole());
        assertEquals("system", messages.get(2).getRole());
        assertTrue(messages.get(2).getContent().contains("Follow-up context"));
        assertEquals("user", messages.get(3).getRole());
        assertEquals("Follow-up question", messages.get(3).getContent());
    }

    @Test
    void injectShouldNotDuplicateSystemMessageWhenRagReturnsEmpty() throws Exception {
        TenantContextHolder.setTenantId("tenant-1");
        List<LlmMessage> messages = new ArrayList<>(List.of(new LlmMessage("user", "Hello")));

        float[] embedding = new float[]{0.1f};
        when(embeddingService.embed("Hello")).thenReturn(embedding);
        when(ragService.searchWithIsolation("tenant-1", null, embedding))
                .thenReturn(List.of());

        contextInjector.inject(messages, 1L);

        assertEquals(1, messages.size());
    }

    @Test
    void injectWithContextShouldReturnOriginalPromptWhenContextIsNull() {
        String prompt = "Hello";
        assertEquals(prompt, contextInjector.injectWithContext(prompt, null));
    }

    @Test
    void injectWithContextShouldReturnOriginalPromptWhenTenantIdIsNull() {
        String prompt = "Hello";
        AgentContext context = AgentContext.builder().agentId(1L).build();
        assertEquals(prompt, contextInjector.injectWithContext(prompt, context));
    }

    @Test
    void injectWithContextShouldReturnEnrichedPromptWhenRagSucceeds() throws Exception {
        String prompt = "What is RAG?";
        AgentContext context = AgentContext.builder()
                .agentId(1L)
                .tenantId("tenant-1")
                .build();

        float[] embedding = new float[]{0.1f};
        when(embeddingService.embed(prompt)).thenReturn(embedding);
        when(ragService.searchWithIsolation("tenant-1", null, embedding))
                .thenReturn(List.of(new SearchResult("RAG is retrieval-augmented generation.", "docs/rag.md", 0.95)));

        String result = contextInjector.injectWithContext(prompt, context);

        assertTrue(result.startsWith("Context from knowledge base:"));
        assertTrue(result.contains("RAG is retrieval-augmented generation."));
        assertTrue(result.contains("User query:"));
        assertTrue(result.contains(prompt));
    }

    @Test
    void injectWithContextShouldReturnOriginalPromptWhenRagFails() throws Exception {
        String prompt = "What is RAG?";
        AgentContext context = AgentContext.builder()
                .agentId(1L)
                .tenantId("tenant-1")
                .build();

        when(embeddingService.embed(prompt)).thenThrow(new RuntimeException("Service unavailable"));

        String result = contextInjector.injectWithContext(prompt, context);

        assertEquals(prompt, result);
    }
}
