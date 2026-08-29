package com.schemaplexai.context.service;

import com.schemaplexai.context.entity.KnowledgeChunk;

import java.util.List;

public interface RagSearchService {

    /**
     * Vector search without document-type filtering.
     */
    List<KnowledgeChunk> search(String query, String tenantId, int topK);

    /**
     * Full spec §4.2 pipeline: query embedding → tenant-scoped ANN search in Milvus →
     * PG back-query by doc_id for document metadata → sorted results.
     *
     * @param query         natural-language query (blank queries return an empty list)
     * @param tenantId      tenant scope (required; null/blank is rejected — never search across tenants)
     * @param topK          maximum number of chunks to return
     * @param docTypeFilter optional docType whitelist applied via the PG back-query; null or empty means no filtering
     */
    List<KnowledgeChunk> search(String query, String tenantId, int topK, List<String> docTypeFilter);
}
