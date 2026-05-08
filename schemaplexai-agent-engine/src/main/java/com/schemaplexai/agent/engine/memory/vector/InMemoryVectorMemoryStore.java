package com.schemaplexai.agent.engine.memory.vector;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory implementation of {@link VectorMemoryStore} using ConcurrentHashMap.
 * Activated when no Milvus-based bean is available.
 * Uses simple keyword matching (substring containment) as a fallback for semantic search.
 */
@Slf4j
@Component
@ConditionalOnMissingBean(name = "milvusVectorMemoryStore")
public class InMemoryVectorMemoryStore implements VectorMemoryStore {

    private final ConcurrentHashMap<String, List<MemoryFragment>> store = new ConcurrentHashMap<>();

    @Override
    public void store(MemoryFragment fragment) {
        validateFragment(fragment);
        String key = buildKey(fragment.agentId(), fragment.tenantId());
        store.computeIfAbsent(key, k -> new ArrayList<>()).add(fragment);
        log.debug("Stored memory fragment {} for agent={}, tenant={}", fragment.id(), fragment.agentId(), fragment.tenantId());
    }

    @Override
    public List<MemoryFragment> retrieve(String agentId, String tenantId, String query, int maxResults) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String key = buildKey(agentId, tenantId);
        List<MemoryFragment> fragments = store.getOrDefault(key, List.of());

        String lowerQuery = query.toLowerCase();
        return fragments.stream()
                .filter(f -> f.content() != null && f.content().toLowerCase().contains(lowerQuery))
                .sorted(Comparator.comparingDouble(MemoryFragment::importance).reversed())
                .limit(maxResults)
                .collect(Collectors.toList());
    }

    @Override
    public void forget(String agentId, String tenantId, Instant before) {
        if (before == null) {
            return;
        }
        String key = buildKey(agentId, tenantId);
        List<MemoryFragment> fragments = store.get(key);
        if (fragments == null) {
            return;
        }
        int removed = 0;
        synchronized (fragments) {
            removed = fragments.size();
            fragments.removeIf(f -> f.createdAt().isBefore(before));
            removed -= fragments.size();
        }
        if (removed > 0) {
            log.debug("Forgot {} memory fragments for agent={}, tenant={} before {}", removed, agentId, tenantId, before);
        }
    }

    @Override
    public List<MemoryFragment> listRecent(String agentId, String tenantId, int limit) {
        String key = buildKey(agentId, tenantId);
        List<MemoryFragment> fragments = store.getOrDefault(key, List.of());
        return fragments.stream()
                .sorted(Comparator.comparing(MemoryFragment::createdAt).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    private String buildKey(String agentId, String tenantId) {
        return agentId + ":" + tenantId;
    }

    private void validateFragment(MemoryFragment fragment) {
        if (fragment == null) {
            throw new IllegalArgumentException("MemoryFragment must not be null");
        }
        if (fragment.agentId() == null || fragment.agentId().isBlank()) {
            throw new IllegalArgumentException("MemoryFragment.agentId must not be blank");
        }
        if (fragment.tenantId() == null || fragment.tenantId().isBlank()) {
            throw new IllegalArgumentException("MemoryFragment.tenantId must not be blank");
        }
        if (fragment.content() == null || fragment.content().isBlank()) {
            throw new IllegalArgumentException("MemoryFragment.content must not be blank");
        }
    }
}
