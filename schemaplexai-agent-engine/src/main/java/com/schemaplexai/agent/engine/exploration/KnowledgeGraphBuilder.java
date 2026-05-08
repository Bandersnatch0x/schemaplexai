package com.schemaplexai.agent.engine.exploration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service that builds and queries a simple in-memory knowledge graph from agent interactions.
 * Supports adding entities, creating relations, and finding paths between entities.
 */
@Slf4j
@Service
public class KnowledgeGraphBuilder {

    private final Map<String, EntityNode> entities = new HashMap<>();
    private final Map<String, List<RelationEdge>> adjacency = new HashMap<>();

    /**
     * Adds an entity to the knowledge graph.
     *
     * @param id         unique entity identifier
     * @param type       entity type (e.g., "Agent", "Task", "Tool")
     * @param properties key-value properties for the entity
     */
    public void addEntity(String id, String type, Map<String, String> properties) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Entity id must not be null or blank");
        }
        String normalizedId = id.trim();
        EntityNode node = new EntityNode(normalizedId, type == null ? "" : type,
                properties == null ? Collections.emptyMap() : Map.copyOf(properties));
        entities.put(normalizedId, node);
        adjacency.putIfAbsent(normalizedId, new ArrayList<>());
        log.debug("Added entity id={}, type={}", normalizedId, node.type());
    }

    /**
     * Adds a directed relation between two entities.
     *
     * @param fromId       the source entity id
     * @param toId         the target entity id
     * @param relationType the type of relation (e.g., "uses", "depends_on", "produces")
     */
    public void addRelation(String fromId, String toId, String relationType) {
        if (fromId == null || fromId.isBlank() || toId == null || toId.isBlank()) {
            throw new IllegalArgumentException("Relation fromId and toId must not be null or blank");
        }
        String normalizedFrom = fromId.trim();
        String normalizedTo = toId.trim();

        if (!entities.containsKey(normalizedFrom)) {
            throw new IllegalArgumentException("Source entity not found: " + normalizedFrom);
        }
        if (!entities.containsKey(normalizedTo)) {
            throw new IllegalArgumentException("Target entity not found: " + normalizedTo);
        }

        RelationEdge edge = new RelationEdge(normalizedFrom, normalizedTo,
                relationType == null ? "" : relationType);
        adjacency.computeIfAbsent(normalizedFrom, k -> new ArrayList<>()).add(edge);
        log.debug("Added relation {} -> {} (type={})", normalizedFrom, normalizedTo, edge.relationType());
    }

    /**
     * Queries a path between two entities using BFS.
     *
     * @param fromId the starting entity id
     * @param toId   the target entity id
     * @return list of relation edges representing the shortest path, or empty list if no path exists
     */
    public List<RelationEdge> queryPath(String fromId, String toId) {
        if (fromId == null || fromId.isBlank() || toId == null || toId.isBlank()) {
            return Collections.emptyList();
        }
        String normalizedFrom = fromId.trim();
        String normalizedTo = toId.trim();

        if (!entities.containsKey(normalizedFrom) || !entities.containsKey(normalizedTo)) {
            return Collections.emptyList();
        }
        if (normalizedFrom.equals(normalizedTo)) {
            return Collections.emptyList();
        }

        Queue<String> queue = new LinkedList<>();
        Map<String, RelationEdge> parentEdge = new HashMap<>();
        Set<String> visited = new HashSet<>();

        queue.add(normalizedFrom);
        visited.add(normalizedFrom);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            List<RelationEdge> edges = adjacency.getOrDefault(current, Collections.emptyList());
            for (RelationEdge edge : edges) {
                if (visited.contains(edge.toId())) {
                    continue;
                }
                parentEdge.put(edge.toId(), edge);
                if (edge.toId().equals(normalizedTo)) {
                    return reconstructPath(parentEdge, normalizedFrom, normalizedTo);
                }
                visited.add(edge.toId());
                queue.add(edge.toId());
            }
        }

        return Collections.emptyList();
    }

    /**
     * Returns all entities in the graph.
     *
     * @return unmodifiable map of entity id to entity node
     */
    public Map<String, EntityNode> getEntities() {
        return Collections.unmodifiableMap(entities);
    }

    /**
     * Returns all relations originating from the given entity.
     *
     * @param entityId the entity id
     * @return unmodifiable list of outgoing relations
     */
    public List<RelationEdge> getRelations(String entityId) {
        if (entityId == null || entityId.isBlank()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(adjacency.getOrDefault(entityId.trim(), Collections.emptyList()));
    }

    private List<RelationEdge> reconstructPath(Map<String, RelationEdge> parentEdge, String fromId, String toId) {
        List<RelationEdge> path = new ArrayList<>();
        String current = toId;
        while (!current.equals(fromId)) {
            RelationEdge edge = parentEdge.get(current);
            if (edge == null) {
                break;
            }
            path.add(edge);
            current = edge.fromId();
        }
        Collections.reverse(path);
        return Collections.unmodifiableList(path);
    }

    /**
     * Record representing an entity node in the knowledge graph.
     *
     * @param id         unique identifier
     * @param type       entity type
     * @param properties key-value properties
     */
    public record EntityNode(String id, String type, Map<String, String> properties) {
    }

    /**
     * Record representing a directed relation edge in the knowledge graph.
     *
     * @param fromId       source entity id
     * @param toId         target entity id
     * @param relationType relation type
     */
    public record RelationEdge(String fromId, String toId, String relationType) {
    }
}
