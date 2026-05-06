package com.schemaplexai.context.service.impl;

import com.schemaplexai.context.config.MilvusProperties;
import com.schemaplexai.context.entity.KnowledgeChunk;
import com.schemaplexai.context.service.RagSearchService;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.response.SearchResp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "milvus.enabled", havingValue = "true", matchIfMissing = false)
public class RagSearchServiceImpl implements RagSearchService {

    private final MilvusClientV2 milvusClient;
    private final MilvusProperties milvusProperties;

    private static final int EMBEDDING_DIMENSION = 1536;
    private static final Random RANDOM = new Random();

    @Override
    public List<KnowledgeChunk> search(String query, String tenantId, int topK) {
        log.info("RAG search: query='{}', tenantId='{}', topK={}", query, tenantId, topK);

        if (query == null || query.isBlank()) {
            return List.of();
        }

        try {
            // Simulate embedding generation for the query
            // TODO: Replace with actual embedding service call
            List<Float> queryEmbedding = generateSimulatedEmbedding();

            SearchReq.SearchReqBuilder searchBuilder = SearchReq.builder()
                    .collectionName(milvusProperties.getCollectionName())
                    .data(List.of(new FloatVec(queryEmbedding)))
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
                            .score((float) result.getScore())
                            .tenantId(getStringValue(entity, "tenant_id"))
                            .build();
                    results.add(chunk);
                }
            }

            log.info("RAG search returned {} results", results.size());
            return results;

        } catch (Exception e) {
            log.error("RAG search failed for query '{}': {}", query, e.getMessage(), e);
            return List.of();
        }
    }

    private List<Float> generateSimulatedEmbedding() {
        List<Float> embedding = new ArrayList<>(EMBEDDING_DIMENSION);
        for (int i = 0; i < EMBEDDING_DIMENSION; i++) {
            embedding.add(RANDOM.nextFloat());
        }
        return embedding;
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
