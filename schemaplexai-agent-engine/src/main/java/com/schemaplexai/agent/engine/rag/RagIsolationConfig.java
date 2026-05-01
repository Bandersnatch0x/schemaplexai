package com.schemaplexai.agent.engine.rag;

/**
 * RAG data isolation configuration.
 * Defines how vector search collections are partitioned by tenant/project.
 */
public record RagIsolationConfig(
        String collectionPrefix,
        boolean enablePartitionIsolation,
        boolean enableSearchFilter,
        int maxResults,
        double similarityThreshold
) {

    public static RagIsolationConfig defaultConfig() {
        return new RagIsolationConfig("tenant_", true, true, 10, 0.7);
    }

    /**
     * Generate a tenant-isolated collection name.
     *
     * @param tenantId tenant identifier
     * @return collection name with tenant prefix
     */
    public String collectionName(String tenantId) {
        return collectionPrefix + tenantId;
    }

    /**
     * Generate a Milvus search filter expression for tenant/project isolation.
     *
     * @param tenantId  tenant identifier
     * @param projectId project identifier (nullable)
     * @return filter expression string
     */
    public String searchFilter(String tenantId, String projectId) {
        StringBuilder filter = new StringBuilder();
        filter.append("tenant_id == '").append(tenantId).append("'");
        if (projectId != null && !projectId.isBlank()) {
            filter.append(" AND project_id == '").append(projectId).append("'");
        }
        return filter.toString();
    }
}
