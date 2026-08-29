package com.schemaplexai.context.service.impl;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.context.config.MilvusProperties;
import com.schemaplexai.context.entity.KnowledgeChunk;
import com.schemaplexai.context.entity.SfKnowledgeDoc;
import com.schemaplexai.context.mapper.SfKnowledgeDocMapper;
import com.schemaplexai.context.service.EmbeddingService;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.ConsistencyLevel;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.response.SearchResp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the vector search pipeline. External dependencies (Milvus, embedding
 * provider, PostgreSQL mapper) are all mocked — no real infrastructure is required.
 */
@ExtendWith(MockitoExtension.class)
class RagSearchServiceImplTest {

    @Mock
    private MilvusClientV2 milvusClient;

    @Mock
    private MilvusProperties milvusProperties;

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private SfKnowledgeDocMapper knowledgeDocMapper;

    @InjectMocks
    private RagSearchServiceImpl ragSearchService;

    @BeforeEach
    void setUp() {
        lenient().when(milvusProperties.getCollectionName()).thenReturn("test_collection");
        lenient().when(milvusProperties.getConsistencyLevel()).thenReturn(ConsistencyLevel.STRONG);
    }

    // ------------------------------------------------------------------
    // input validation / fail-closed tenant boundary
    // ------------------------------------------------------------------

    @Test
    void search_nullQuery_returnsEmptyList() {
        List<KnowledgeChunk> result = ragSearchService.search(null, "tenant1", 5);

        assertThat(result).isEmpty();
        verifyNoInteractions(milvusClient, embeddingService, knowledgeDocMapper);
    }

    @Test
    void search_blankQuery_returnsEmptyList() {
        List<KnowledgeChunk> result = ragSearchService.search("   ", "tenant1", 5);

        assertThat(result).isEmpty();
        verifyNoInteractions(milvusClient, embeddingService, knowledgeDocMapper);
    }

    @Test
    void search_nullTenantId_throwsParamErrorInsteadOfCrossTenantSearch() {
        assertThatThrownBy(() -> ragSearchService.search("test", null, 5))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> {
                    BaseException be = (BaseException) ex;
                    assertThat(be.getCode()).isEqualTo(ResultCode.PARAM_ERROR.getCode());
                    assertThat(be.getMessage()).contains("tenantId is required");
                });

        verifyNoInteractions(embeddingService, milvusClient, knowledgeDocMapper);
    }

    @Test
    void search_blankTenantId_throwsParamErrorInsteadOfCrossTenantSearch() {
        assertThatThrownBy(() -> ragSearchService.search("test", "   ", 5))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> assertThat(((BaseException) ex).getCode())
                        .isEqualTo(ResultCode.PARAM_ERROR.getCode()));

        verifyNoInteractions(embeddingService, milvusClient, knowledgeDocMapper);
    }

    @Test
    void search_invalidTenantIdFormat_throwsParamErrorBeforeEmbedding() {
        assertThatThrownBy(() -> ragSearchService.search("test", "tenant;id", 5))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> {
                    BaseException be = (BaseException) ex;
                    assertThat(be.getCode()).isEqualTo(ResultCode.PARAM_ERROR.getCode());
                    assertThat(be.getMessage()).contains("Invalid tenantId format");
                });

        verifyNoInteractions(embeddingService, milvusClient);
    }

    @Test
    void search_embeddingFailure_throwsBaseException() {
        when(embeddingService.embed("test query")).thenThrow(new RuntimeException("embedding failed"));

        assertThatThrownBy(() -> ragSearchService.search("test query", "tenant1", 5))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> {
                    BaseException be = (BaseException) ex;
                    assertThat(be.getCode()).isEqualTo(ResultCode.INTERNAL_ERROR.getCode());
                    assertThat(be.getMessage()).contains("Embedding generation failed");
                });
    }

    @Test
    void search_milvusFailure_throwsBaseException() {
        when(embeddingService.embed("test")).thenReturn(new float[]{1.0f, 0.0f});
        when(milvusClient.search(any(SearchReq.class))).thenThrow(new RuntimeException("milvus error"));

        assertThatThrownBy(() -> ragSearchService.search("test", "tenant1", 5))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> {
                    BaseException be = (BaseException) ex;
                    assertThat(be.getCode()).isEqualTo(ResultCode.INTERNAL_ERROR.getCode());
                    assertThat(be.getMessage()).contains("RAG search failed");
                });
    }

    // ------------------------------------------------------------------
    // ANN search behaviour
    // ------------------------------------------------------------------

    @Test
    void search_noResults_returnsEmptyList() {
        when(embeddingService.embed("test")).thenReturn(new float[]{1.0f, 0.0f});
        SearchResp response = mock(SearchResp.class);
        when(response.getSearchResults()).thenReturn(Collections.emptyList());
        when(milvusClient.search(any(SearchReq.class))).thenReturn(response);

        List<KnowledgeChunk> result = ragSearchService.search("test", "tenant1", 5);

        assertThat(result).isEmpty();
        verifyNoInteractions(knowledgeDocMapper);
    }

    @Test
    void search_alwaysAppliesTenantFilter() {
        when(embeddingService.embed("test")).thenReturn(new float[]{1.0f, 0.0f});
        SearchResp response = mock(SearchResp.class);
        when(response.getSearchResults()).thenReturn(Collections.emptyList());
        when(milvusClient.search(any(SearchReq.class))).thenReturn(response);

        ragSearchService.search("test", "tenant-123", 5);

        ArgumentCaptor<SearchReq> captor = ArgumentCaptor.forClass(SearchReq.class);
        verify(milvusClient).search(captor.capture());
        assertThat(captor.getValue().getFilter()).isEqualTo("tenant_id == \"tenant-123\"");
    }

    @Test
    void search_withTenantId_escapesQuotesViaValidation() {
        // The quote in tenantId will fail validation (pattern doesn't allow quotes)
        // so it throws before reaching milvusClient.search
        assertThatThrownBy(() -> ragSearchService.search("test", "ten\"ant", 5))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> assertThat(ex.getMessage()).contains("Invalid tenantId format"));

        verifyNoInteractions(embeddingService, milvusClient);
    }

    @Test
    void search_topK_passedThrough() {
        when(embeddingService.embed("test")).thenReturn(new float[]{1.0f});
        SearchResp response = mock(SearchResp.class);
        when(response.getSearchResults()).thenReturn(Collections.emptyList());
        when(milvusClient.search(any(SearchReq.class))).thenReturn(response);

        ragSearchService.search("test", "tenant1", 10);

        ArgumentCaptor<SearchReq> captor = ArgumentCaptor.forClass(SearchReq.class);
        verify(milvusClient).search(captor.capture());
        assertThat(captor.getValue().getTopK()).isEqualTo(10);
    }

    @Test
    void search_withResults_returnsMappedChunks() {
        when(embeddingService.embed("test")).thenReturn(new float[]{1.0f, 0.0f});

        SearchResp.SearchResult result1 = mock(SearchResp.SearchResult.class);
        when(result1.getEntity()).thenReturn(Map.of(
                "doc_id", "doc-1",
                "chunk_index", 0,
                "content", "content1",
                "tenant_id", "t1"
        ));
        when(result1.getDistance()).thenReturn(0.95f);

        SearchResp.SearchResult result2 = mock(SearchResp.SearchResult.class);
        when(result2.getEntity()).thenReturn(Map.of(
                "doc_id", "doc-2",
                "chunk_index", 1,
                "content", "content2",
                "tenant_id", "t2"
        ));
        when(result2.getDistance()).thenReturn(null);

        SearchResp response = mock(SearchResp.class);
        when(response.getSearchResults()).thenReturn(List.of(List.of(result1, result2)));
        when(milvusClient.search(any(SearchReq.class))).thenReturn(response);

        List<KnowledgeChunk> results = ragSearchService.search("test", "tenant1", 5);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).getDocId()).isEqualTo("doc-1");
        assertThat(results.get(0).getChunkIndex()).isEqualTo(0);
        assertThat(results.get(0).getContent()).isEqualTo("content1");
        assertThat(results.get(0).getScore()).isEqualTo(0.95f);
        assertThat(results.get(0).getTenantId()).isEqualTo("t1");

        assertThat(results.get(1).getDocId()).isEqualTo("doc-2");
        assertThat(results.get(1).getScore()).isEqualTo(0.0f);
    }

    @Test
    void search_multipleResultLists_flattensAll() {
        when(embeddingService.embed("test")).thenReturn(new float[]{1.0f});

        SearchResp.SearchResult r1 = mock(SearchResp.SearchResult.class);
        when(r1.getEntity()).thenReturn(Map.of("doc_id", "d1", "content", "c1"));
        when(r1.getDistance()).thenReturn(0.9f);

        SearchResp.SearchResult r2 = mock(SearchResp.SearchResult.class);
        when(r2.getEntity()).thenReturn(Map.of("doc_id", "d2", "content", "c2"));
        when(r2.getDistance()).thenReturn(0.8f);

        SearchResp response = mock(SearchResp.class);
        when(response.getSearchResults()).thenReturn(List.of(List.of(r1), List.of(r2)));
        when(milvusClient.search(any(SearchReq.class))).thenReturn(response);

        List<KnowledgeChunk> results = ragSearchService.search("test", "tenant1", 5);

        assertThat(results).hasSize(2);
    }

    @Test
    void search_nullEntityValues_handlesGracefully() {
        when(embeddingService.embed("test")).thenReturn(new float[]{1.0f});

        SearchResp.SearchResult result = mock(SearchResp.SearchResult.class);
        when(result.getEntity()).thenReturn(Map.of());
        when(result.getDistance()).thenReturn(0.5f);

        SearchResp response = mock(SearchResp.class);
        when(response.getSearchResults()).thenReturn(List.of(List.of(result)));
        when(milvusClient.search(any(SearchReq.class))).thenReturn(response);

        List<KnowledgeChunk> results = ragSearchService.search("test", "tenant1", 5);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getDocId()).isNull();
        assertThat(results.get(0).getChunkIndex()).isNull();
        assertThat(results.get(0).getContent()).isNull();
        assertThat(results.get(0).getScore()).isEqualTo(0.5f);
    }

    @Test
    void search_numericChunkIndex_parsesCorrectly() {
        when(embeddingService.embed("test")).thenReturn(new float[]{1.0f});

        SearchResp.SearchResult result = mock(SearchResp.SearchResult.class);
        when(result.getEntity()).thenReturn(Map.of(
                "doc_id", "doc-1",
                "chunk_index", 42L,
                "content", "text"
        ));
        when(result.getDistance()).thenReturn(0.5f);

        SearchResp response = mock(SearchResp.class);
        when(response.getSearchResults()).thenReturn(List.of(List.of(result)));
        when(milvusClient.search(any(SearchReq.class))).thenReturn(response);

        List<KnowledgeChunk> results = ragSearchService.search("test", "tenant1", 5);

        assertThat(results.get(0).getChunkIndex()).isEqualTo(42);
    }

    @Test
    void search_stringChunkIndex_parsesCorrectly() {
        when(embeddingService.embed("test")).thenReturn(new float[]{1.0f});

        SearchResp.SearchResult result = mock(SearchResp.SearchResult.class);
        when(result.getEntity()).thenReturn(Map.of(
                "doc_id", "doc-1",
                "chunk_index", "7",
                "content", "text"
        ));
        when(result.getDistance()).thenReturn(0.5f);

        SearchResp response = mock(SearchResp.class);
        when(response.getSearchResults()).thenReturn(List.of(List.of(result)));
        when(milvusClient.search(any(SearchReq.class))).thenReturn(response);

        List<KnowledgeChunk> results = ragSearchService.search("test", "tenant1", 5);

        assertThat(results.get(0).getChunkIndex()).isEqualTo(7);
    }

    @Test
    void search_invalidStringChunkIndex_returnsNull() {
        when(embeddingService.embed("test")).thenReturn(new float[]{1.0f});

        SearchResp.SearchResult result = mock(SearchResp.SearchResult.class);
        when(result.getEntity()).thenReturn(Map.of(
                "doc_id", "doc-1",
                "chunk_index", "not-a-number",
                "content", "text"
        ));
        when(result.getDistance()).thenReturn(0.5f);

        SearchResp response = mock(SearchResp.class);
        when(response.getSearchResults()).thenReturn(List.of(List.of(result)));
        when(milvusClient.search(any(SearchReq.class))).thenReturn(response);

        List<KnowledgeChunk> results = ragSearchService.search("test", "tenant1", 5);

        assertThat(results.get(0).getChunkIndex()).isNull();
    }

    // ------------------------------------------------------------------
    // step 3: PG back-query enrichment
    // ------------------------------------------------------------------

    @Test
    void search_enrichesChunksWithDocumentMetadataFromPg() {
        when(embeddingService.embed("test")).thenReturn(new float[]{1.0f});
        SearchResp.SearchResult result = mock(SearchResp.SearchResult.class);
        when(result.getEntity()).thenReturn(Map.of("doc_id", "101", "content", "chunk body"));
        when(result.getDistance()).thenReturn(0.9f);
        SearchResp response = mock(SearchResp.class);
        when(response.getSearchResults()).thenReturn(List.of(List.of(result)));
        when(milvusClient.search(any(SearchReq.class))).thenReturn(response);

        SfKnowledgeDoc doc = new SfKnowledgeDoc();
        doc.setId(101L);
        doc.setTitle("Spring Boot Guide");
        doc.setDocType("technical_doc");
        when(knowledgeDocMapper.selectBatchIds(any())).thenReturn(List.of(doc));

        List<KnowledgeChunk> results = ragSearchService.search("test", "tenant1", 5);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getDocName()).isEqualTo("Spring Boot Guide");
        assertThat(results.get(0).getMetadata()).containsEntry("docType", "technical_doc");
    }

    @Test
    void search_docTypeFilter_excludesChunksOfOtherTypes() {
        when(embeddingService.embed("test")).thenReturn(new float[]{1.0f});

        SearchResp.SearchResult match = mock(SearchResp.SearchResult.class);
        when(match.getEntity()).thenReturn(Map.of("doc_id", "101", "content", "matching"));
        when(match.getDistance()).thenReturn(0.9f);
        SearchResp.SearchResult other = mock(SearchResp.SearchResult.class);
        when(other.getEntity()).thenReturn(Map.of("doc_id", "102", "content", "non-matching"));
        when(other.getDistance()).thenReturn(0.8f);

        SearchResp response = mock(SearchResp.class);
        when(response.getSearchResults()).thenReturn(List.of(List.of(match, other)));
        when(milvusClient.search(any(SearchReq.class))).thenReturn(response);

        SfKnowledgeDoc technical = new SfKnowledgeDoc();
        technical.setId(101L);
        technical.setTitle("Technical");
        technical.setDocType("technical_doc");
        SfKnowledgeDoc memo = new SfKnowledgeDoc();
        memo.setId(102L);
        memo.setTitle("Memo");
        memo.setDocType("memo");
        when(knowledgeDocMapper.selectBatchIds(any())).thenReturn(List.of(technical, memo));

        List<KnowledgeChunk> results =
                ragSearchService.search("test", "tenant1", 5, List.of("technical_doc"));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getDocId()).isEqualTo("101");
    }

    @Test
    void search_orphanChunkWithMissingDocument_isDropped() {
        // A Milvus row whose document is not visible to the tenant (deleted or foreign)
        // must not be returned — its tenant ownership cannot be verified.
        when(embeddingService.embed("test")).thenReturn(new float[]{1.0f});
        SearchResp.SearchResult orphan = mock(SearchResp.SearchResult.class);
        when(orphan.getEntity()).thenReturn(Map.of("doc_id", "999", "content", "orphan"));
        when(orphan.getDistance()).thenReturn(0.7f);
        SearchResp response = mock(SearchResp.class);
        when(response.getSearchResults()).thenReturn(List.of(List.of(orphan)));
        when(milvusClient.search(any(SearchReq.class))).thenReturn(response);

        when(knowledgeDocMapper.selectBatchIds(any())).thenReturn(List.of());

        List<KnowledgeChunk> results = ragSearchService.search("test", "tenant1", 5);

        assertThat(results).isEmpty();
    }

    @Test
    void search_legacyUnparseableDocId_isKeptWithoutMetadata() {
        when(embeddingService.embed("test")).thenReturn(new float[]{1.0f});
        SearchResp.SearchResult legacy = mock(SearchResp.SearchResult.class);
        when(legacy.getEntity()).thenReturn(Map.of("doc_id", "legacy-uuid", "content", "legacy"));
        when(legacy.getDistance()).thenReturn(0.6f);
        SearchResp response = mock(SearchResp.class);
        when(response.getSearchResults()).thenReturn(List.of(List.of(legacy)));
        when(milvusClient.search(any(SearchReq.class))).thenReturn(response);

        List<KnowledgeChunk> results = ragSearchService.search("test", "tenant1", 5);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getDocName()).isNull();
        assertThat(results.get(0).getMetadata()).isNull();
        verifyNoInteractions(knowledgeDocMapper);
    }
}
