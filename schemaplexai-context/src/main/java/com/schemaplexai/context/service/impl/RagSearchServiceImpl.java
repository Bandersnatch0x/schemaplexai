package com.schemaplexai.context.service.impl;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.context.config.MilvusProperties;
import com.schemaplexai.context.entity.KnowledgeChunk;
import com.schemaplexai.context.entity.SfKnowledgeDoc;
import com.schemaplexai.context.mapper.SfKnowledgeDocMapper;
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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Vector search pipeline (spec §4.2):
 * <ol>
 *   <li>Embedding service turns the query into a vector</li>
 *   <li>Milvus ANN search with a mandatory tenant_id filter</li>
 *   <li>PG back-query by doc_id for full document metadata</li>
 *   <li>Sorted results returned to the caller</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "milvus.enabled", havingValue = "true", matchIfMissing = false)
public class RagSearchServiceImpl implements RagSearchService {

    private static final Pattern TENANT_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9\\-]+$");

    private final MilvusClientV2 milvusClient;
    private final MilvusProperties milvusProperties;
    private final EmbeddingService embeddingService;
    private final SfKnowledgeDocMapper knowledgeDocMapper;

    @Override
    public List<KnowledgeChunk> search(String query, String tenantId, int topK) {
        return search(query, tenantId, topK, null);
    }

    @Override
    public List<KnowledgeChunk> search(String query, String tenantId, int topK, List<String> docTypeFilter) {
        log.info("RAG search: query='{}', tenantId='{}', topK={}", query, tenantId, topK);

        if (query == null || query.isBlank()) {
            return List.of();
        }

        // Fail-closed tenant boundary: a missing tenant must never fall through to an
        // unfiltered (cross-tenant) ANN search.
        if (tenantId == null || tenantId.isBlank()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "tenantId is required for RAG search");
        }
        validateTenantId(tenantId);

        // Step 1: generate real embedding for query — failure must propagate, never silently fallback.
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

        List<KnowledgeChunk> results;
        try {
            // Step 2: tenant-scoped ANN search in Milvus (tenantId validated non-blank above).
            SearchReq searchReq = SearchReq.builder()
                    .collectionName(milvusProperties.getCollectionName())
                    .data(List.of(queryEmbedding))
                    .topK(topK)
                    .outputFields(List.of("doc_id", "chunk_index", "content", "tenant_id"))
                    .consistencyLevel(milvusProperties.getConsistencyLevel())
                    .filter(String.format("tenant_id == \"%s\"", tenantId))
                    .build();

            SearchResp response = milvusClient.search(searchReq);

            results = new ArrayList<>();
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
        } catch (Exception e) {
            log.error("RAG search failed for query '{}': {}", query, e.getMessage(), e);
            throw new BaseException(ResultCode.INTERNAL_ERROR,
                    "RAG search failed: " + e.getMessage(), e);
        }

        log.info("RAG search returned {} raw results", results.size());

        // Step 3: PG back-query by doc_id for full document metadata (+ optional docType filter).
        // Step 4: results keep the Milvus similarity ordering.
        return enrichWithDocumentMetadata(results, docTypeFilter);
    }

    /**
     * Step 3 of spec §4.2: resolve document metadata from PG for every chunk whose doc_id
     * parses to a PG primary key. The PG query is tenant-scoped by the DAO tenant interceptor.
     * Chunks pointing at documents invisible to the current tenant (deleted or foreign) are
     * dropped — fail-closed, since their tenant ownership cannot be verified.
     */
    private List<KnowledgeChunk> enrichWithDocumentMetadata(List<KnowledgeChunk> chunks, List<String> docTypeFilter) {
        if (chunks.isEmpty()) {
            return chunks;
        }

        Set<Long> docIds = new LinkedHashSet<>();
        for (KnowledgeChunk chunk : chunks) {
            Long docId = parseDocId(chunk.getDocId());
            if (docId != null) {
                docIds.add(docId);
            }
        }

        Map<Long, SfKnowledgeDoc> docsById;
        if (docIds.isEmpty()) {
            docsById = Map.of();
        } else {
            List<SfKnowledgeDoc> docs = knowledgeDocMapper.selectBatchIds(docIds);
            docsById = docs == null ? Map.of()
                    : docs.stream().collect(Collectors.toMap(SfKnowledgeDoc::getId, Function.identity(), (a, b) -> a));
        }

        boolean filterActive = docTypeFilter != null && !docTypeFilter.isEmpty();
        List<KnowledgeChunk> enriched = new ArrayList<>(chunks.size());
        for (KnowledgeChunk chunk : chunks) {
            Long docId = parseDocId(chunk.getDocId());
            if (docId == null) {
                // Legacy/unparseable doc_id: keep the chunk without metadata.
                enriched.add(chunk);
                continue;
            }
            SfKnowledgeDoc doc = docsById.get(docId);
            if (doc == null) {
                log.warn("RAG search dropping orphan chunk: doc_id={} not visible to the current tenant",
                        chunk.getDocId());
                continue;
            }
            chunk.setDocName(doc.getTitle());
            Map<String, Object> metadata = new LinkedHashMap<>();
            if (doc.getDocType() != null) {
                metadata.put("docType", doc.getDocType());
            }
            chunk.setMetadata(metadata);
            if (filterActive && !docTypeFilter.contains(doc.getDocType())) {
                continue;
            }
            enriched.add(chunk);
        }
        return enriched;
    }

    private Long parseDocId(String docId) {
        if (docId == null || docId.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(docId.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void validateTenantId(String tenantId) {
        if (!TENANT_ID_PATTERN.matcher(tenantId).matches()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Invalid tenantId format");
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
