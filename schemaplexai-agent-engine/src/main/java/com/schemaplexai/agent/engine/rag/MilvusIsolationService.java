package com.schemaplexai.agent.engine.rag;

import com.schemaplexai.agent.engine.context.ValidationResult;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Milvus isolation service.
 * Enforces multi-tenant data isolation for RAG vector searches
 * via collection-level partitioning and search-time filter expressions.
 */
@Slf4j
public class MilvusIsolationService {

    private final RagIsolationConfig config;
    private final MilvusClient milvusClient;

    public MilvusIsolationService(RagIsolationConfig config, MilvusClient milvusClient) {
        this.config = config;
        this.milvusClient = milvusClient;
    }

    /**
     * Validate that the given tenant is allowed to perform RAG searches.
     *
     * @param tenantId  tenant identifier
     * @param projectId project identifier (nullable)
     * @return validation result
     */
    public ValidationResult validateSearchPermission(String tenantId, String projectId) {
        if (tenantId == null || tenantId.isBlank()) {
            return ValidationResult.invalid("Tenant ID is required for RAG search");
        }

        if (config.enablePartitionIsolation()) {
            String collectionName = config.collectionName(tenantId);
            if (!milvusClient.hasCollection(collectionName)) {
                return ValidationResult.invalid("Collection not found: " + collectionName);
            }
        }

        return ValidationResult.valid();
    }

    /**
     * Execute a tenant-isolated vector search.
     *
     * @param tenantId       tenant identifier
     * @param projectId      project identifier (nullable)
     * @param queryEmbedding query vector
     * @return list of search results
     * @throws SecurityException if permission validation fails
     */
    public List<SearchResult> searchWithIsolation(String tenantId, String projectId,
                                                   float[] queryEmbedding) {
        ValidationResult validation = validateSearchPermission(tenantId, projectId);
        if (!validation.isValid()) {
            throw new SecurityException("RAG search permission denied: " + validation.getErrorMessage());
        }

        String collectionName = config.collectionName(tenantId);
        String filter = config.enableSearchFilter()
                ? config.searchFilter(tenantId, projectId)
                : null;

        log.debug("RAG search: collection={}, tenant={}, project={}, filter={}",
                collectionName, tenantId, projectId, filter);

        return milvusClient.search(
                collectionName,
                queryEmbedding,
                config.maxResults(),
                config.similarityThreshold(),
                filter
        );
    }
}
