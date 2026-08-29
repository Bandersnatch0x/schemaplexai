package com.schemaplexai.context.controller;

import com.schemaplexai.common.context.TenantContextHolder;
import com.schemaplexai.common.result.Result;
import com.schemaplexai.context.dto.RagSearchItem;
import com.schemaplexai.context.entity.KnowledgeChunk;
import com.schemaplexai.context.service.RagSearchService;
import com.schemaplexai.context.service.RagService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RagControllerTest {

    @Mock
    private RagService ragService;

    @Mock
    private ObjectProvider<RagSearchService> ragSearchServiceProvider;

    @Mock
    private RagSearchService ragSearchService;

    @InjectMocks
    private RagController controller;

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void retrieve_success() {
        when(ragService.retrieve("test query", 5)).thenReturn(List.of("chunk1", "chunk2"));
        RagController.RetrieveRequest request = new RagController.RetrieveRequest();
        request.setQuery("test query");
        request.setTopK(5);
        Result<List<String>> result = controller.retrieve(request);
        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).containsExactly("chunk1", "chunk2");
    }

    // ------------------------------------------------------------------
    // POST /context/rag/search
    // ------------------------------------------------------------------

    @Test
    void search_vectorSearchDisabled_returns503() {
        TenantContextHolder.setTenantId("tenant-1");
        when(ragSearchServiceProvider.getIfAvailable()).thenReturn(null);

        RagController.RagSearchRequest request = new RagController.RagSearchRequest();
        request.setQuery("query");

        Result<List<RagSearchItem>> result = controller.search(request);

        assertThat(result.getCode()).isEqualTo(503);
        verifyNoInteractions(ragSearchService);
    }

    @Test
    void search_missingTenant_returns400() {
        // No tenant in the holder -> rejected before consulting the search service.
        RagController.RagSearchRequest request = new RagController.RagSearchRequest();
        request.setQuery("query");

        Result<List<RagSearchItem>> result = controller.search(request);

        assertThat(result.getCode()).isEqualTo(400);
        assertThat(result.getMessage()).contains("Tenant ID is required");
        verify(ragSearchService, never()).search(anyString(), anyString(), anyInt(), any());
        verifyNoInteractions(ragSearchServiceProvider);
    }

    @Test
    void search_blankQuery_returns400() {
        TenantContextHolder.setTenantId("tenant-1");

        RagController.RagSearchRequest request = new RagController.RagSearchRequest();
        request.setQuery("   ");

        Result<List<RagSearchItem>> result = controller.search(request);

        assertThat(result.getCode()).isEqualTo(400);
        verifyNoInteractions(ragSearchService);
    }

    @Test
    void search_nullBody_returns400() {
        TenantContextHolder.setTenantId("tenant-1");

        Result<List<RagSearchItem>> result = controller.search(null);

        assertThat(result.getCode()).isEqualTo(400);
        verifyNoInteractions(ragSearchService);
    }

    @Test
    void search_success_mapsChunksToItemsWithMetadata() {
        TenantContextHolder.setTenantId("tenant-1");
        when(ragSearchServiceProvider.getIfAvailable()).thenReturn(ragSearchService);
        KnowledgeChunk chunk = KnowledgeChunk.builder()
                .docId("101")
                .docName("Spring Boot Guide")
                .content("chunk body")
                .score(0.92f)
                .metadata(Map.of("docType", "technical_doc"))
                .build();
        when(ragSearchService.search("query", "tenant-1", 5, null)).thenReturn(List.of(chunk));

        RagController.RagSearchRequest request = new RagController.RagSearchRequest();
        request.setQuery("query");

        Result<List<RagSearchItem>> result = controller.search(request);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).hasSize(1);
        RagSearchItem item = result.getData().get(0);
        assertThat(item.getDocId()).isEqualTo(101L);
        assertThat(item.getDocName()).isEqualTo("Spring Boot Guide");
        assertThat(item.getContent()).isEqualTo("chunk body");
        assertThat(item.getScore()).isEqualTo(0.92f);
        assertThat(item.getMetadata()).containsEntry("docType", "technical_doc");
    }

    @Test
    void search_passesDocTypeFilterAndCustomTopK() {
        TenantContextHolder.setTenantId("tenant-1");
        when(ragSearchServiceProvider.getIfAvailable()).thenReturn(ragSearchService);
        when(ragSearchService.search(anyString(), anyString(), anyInt(), any())).thenReturn(List.of());

        RagController.RagSearchRequest request = new RagController.RagSearchRequest();
        request.setQuery("query");
        request.setTopK(3);
        RagController.RagSearchFilters filters = new RagController.RagSearchFilters();
        filters.setDocType(List.of("technical_doc"));
        request.setFilters(filters);

        controller.search(request);

        verify(ragSearchService).search("query", "tenant-1", 3, List.of("technical_doc"));
    }

    @Test
    void search_nonNumericDocId_returnsNullDocIdItem() {
        TenantContextHolder.setTenantId("tenant-1");
        when(ragSearchServiceProvider.getIfAvailable()).thenReturn(ragSearchService);
        KnowledgeChunk chunk = KnowledgeChunk.builder()
                .docId("legacy-uuid")
                .content("legacy body")
                .score(0.5f)
                .build();
        when(ragSearchService.search(anyString(), anyString(), anyInt(), any())).thenReturn(List.of(chunk));

        RagController.RagSearchRequest request = new RagController.RagSearchRequest();
        request.setQuery("query");

        Result<List<RagSearchItem>> result = controller.search(request);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData().get(0).getDocId()).isNull();
    }
}
