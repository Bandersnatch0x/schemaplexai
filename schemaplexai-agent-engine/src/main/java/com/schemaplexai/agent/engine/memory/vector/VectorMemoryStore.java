package com.schemaplexai.agent.engine.memory.vector;

import java.time.Instant;
import java.util.List;

/**
 * Interface for storing and retrieving long-term vector memory fragments.
 * Implementations may use in-memory storage, Milvus, or other vector databases.
 */
public interface VectorMemoryStore {

    /**
     * Store a memory fragment.
     *
     * @param fragment the fragment to store
     */
    void store(MemoryFragment fragment);

    /**
     * Retrieve memory fragments semantically similar to the query.
     *
     * @param agentId    the agent to search within
     * @param tenantId   the tenant to search within
     * @param query      the search query
     * @param maxResults maximum number of results to return
     * @return list of relevant memory fragments, ordered by relevance
     */
    List<MemoryFragment> retrieve(String agentId, String tenantId, String query, int maxResults);

    /**
     * Remove memories older than the given cutoff time.
     *
     * @param agentId  the agent whose memories to prune
     * @param tenantId the tenant whose memories to prune
     * @param before   remove memories created before this instant
     */
    void forget(String agentId, String tenantId, Instant before);

    /**
     * List the most recent memory fragments for an agent.
     *
     * @param agentId    the agent to list memories for
     * @param tenantId   the tenant to list memories for
     * @param limit      maximum number of fragments to return
     * @return list of recent memory fragments, most recent first
     */
    List<MemoryFragment> listRecent(String agentId, String tenantId, int limit);
}
