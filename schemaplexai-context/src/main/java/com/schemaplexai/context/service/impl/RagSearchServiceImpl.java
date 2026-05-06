package com.schemaplexai.context.service.impl;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.context.config.MilvusProperties;
import com.schemaplexai.context.entity.KnowledgeChunk;
import com.schemaplexai.context.service.EmbeddingService;
import com.schemaplexai.context.service.RagSearchService;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.response.SearchResp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "milvus.enabled", havingValue = "true", matchIfMissing = false)
public class RagSearchServiceImpl implements RagSearchService {

    private final MilvusClientV2 milvusClient;
    private final MilvusProperties milvusProperties;
    private final EmbeddingService embeddingService;

    @Override
    public List<KnowledgeChunk> search(String query, String tenantId, int topK) {
        log.info("RAG search: query='{}', tenantId='{}', topK={}", query, tenantId, topK);

        if (query == null || query.isBlank()) {
            return List.of();
        }

        // Generate real embedding for query — failure must propagate, never silently fallback.
        List<Float> queryEmbedding;
        try {
            float[] qEmb = embeddingService.embed(query);
            queryEmbedding = new ArrayList<>(qEmb.length);
            for (float v : qEmb) {
                queryEmbedding.add(v);
            }
        } catch (Exception e) {
            log.error("Embedding generation failed for query '{}': {}", query, e.getMessage(), e);
            throw new BaseException(ResultCode.INTERNAL_ERROR,
                    "Embedding generation failed for RAG query: " + e.getMessage(), e);
        }

        try {
            SearchReq.SearchReqBuilder searchBuilder = SearchReq.builder()
                    .collectionName(milvusProperties.getCollectionName())
                    .data(List.of(queryEmbedding))
                    .topK(topK)
                    .outputFields(List.of("doc_id", "chunk_index", "content", "tenant_id"));

            if (tenantId != null && !tenantId.isBlank()) {
                searchBuilder.filter("tenant_id == '" + tenantId + "'");
            }

            SearchResp response = milvusClient.search(searchBuilder.build());

            List<KnowledgeChunk> results = new ArrayList<>();
            List<List<SearchResp.SearchResult>> searchResults = response.getSearchResults();

            for (List<SearchResp.SearchResult> resultList : searchResults) {
                for (SearchResp.SearchResult result : resultList) {
                    Map<String, Object> entity = result.getEntity();
                    KnowledgeChunk chunk = KnowledgeChunk.builder()
                            .docId(getStringValue(entity, "doc_id"))
                            .chunkIndex(getIntValue(entity, "chunk_index"))
                            .content(getStringValue(entity, "content"))
                            .score(result.getDistance() != null ? result.getDistance() : 0.0f)
                            .tenantId(getStringValue(entity, "tenant_id"))
                            .build();
                    results.add(chunk);
                }
            }

            log.info("RAG search returned {} results", results.size());
            return results;

        } catch (Exception e) {
            log.error("RAG search failed for query '{}': {}", query, e.getMessage(), e);
            throw new BaseException(ResultCode.INTERNAL_ERROR,
                    "RAG search failed: " + e.getMessage(), e);
        }
    }

    private String getStringValue(Map<String, Object> entity, String key) {
        Object value = entity.get(key);
        return value != null ? value.toString() : null;
    }

    private Integer getIntValue(Map<String, Object> entity, String key) {
        Object value = entity.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
