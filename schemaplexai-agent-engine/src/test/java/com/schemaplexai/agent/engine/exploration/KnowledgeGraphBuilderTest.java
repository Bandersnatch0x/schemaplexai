package com.schemaplexai.agent.engine.exploration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("KnowledgeGraphBuilder")
class KnowledgeGraphBuilderTest {

    private KnowledgeGraphBuilder graph;

    @BeforeEach
    void setUp() {
        graph = new KnowledgeGraphBuilder();
    }

    @Nested
    @DisplayName("addEntity")
    class AddEntityTests {

        @Test
        @DisplayName("should add entity with properties")
        void shouldAddEntityWithProperties() {
            graph.addEntity("agent-1", "Agent", Map.of("name", "Alpha"));

            assertEquals(1, graph.getEntities().size());
            assertEquals("Agent", graph.getEntities().get("agent-1").type());
            assertEquals("Alpha", graph.getEntities().get("agent-1").properties().get("name"));
        }

        @Test
        @DisplayName("should add entity with null properties as empty map")
        void shouldAddEntityWithNullProperties() {
            graph.addEntity("agent-2", "Agent", null);

            assertThat(graph.getEntities().get("agent-2").properties()).isEmpty();
        }

        @Test
        @DisplayName("should throw for null id")
        void shouldThrowForNullId() {
            assertThrows(IllegalArgumentException.class, () ->
                    graph.addEntity(null, "Agent", Collections.emptyMap()));
        }

        @Test
        @DisplayName("should throw for blank id")
        void shouldThrowForBlankId() {
            assertThrows(IllegalArgumentException.class, () ->
                    graph.addEntity("   ", "Agent", Collections.emptyMap()));
        }
    }

    @Nested
    @DisplayName("addRelation")
    class AddRelationTests {

        @Test
        @DisplayName("should add relation between existing entities")
        void shouldAddRelation() {
            graph.addEntity("a", "Task", Collections.emptyMap());
            graph.addEntity("b", "Tool", Collections.emptyMap());
            graph.addRelation("a", "b", "uses");

            List<KnowledgeGraphBuilder.RelationEdge> relations = graph.getRelations("a");
            assertEquals(1, relations.size());
            assertEquals("uses", relations.get(0).relationType());
            assertEquals("b", relations.get(0).toId());
        }

        @Test
        @DisplayName("should throw when source entity does not exist")
        void shouldThrowForMissingSource() {
            graph.addEntity("b", "Tool", Collections.emptyMap());
            assertThrows(IllegalArgumentException.class, () ->
                    graph.addRelation("a", "b", "uses"));
        }

        @Test
        @DisplayName("should throw when target entity does not exist")
        void shouldThrowForMissingTarget() {
            graph.addEntity("a", "Task", Collections.emptyMap());
            assertThrows(IllegalArgumentException.class, () ->
                    graph.addRelation("a", "b", "uses"));
        }

        @Test
        @DisplayName("should throw for null fromId")
        void shouldThrowForNullFromId() {
            assertThrows(IllegalArgumentException.class, () ->
                    graph.addRelation(null, "b", "uses"));
        }

        @Test
        @DisplayName("should throw for blank toId")
        void shouldThrowForBlankToId() {
            assertThrows(IllegalArgumentException.class, () ->
                    graph.addRelation("a", "   ", "uses"));
        }
    }

    @Nested
    @DisplayName("queryPath")
    class QueryPathTests {

        @Test
        @DisplayName("should find direct path")
        void shouldFindDirectPath() {
            graph.addEntity("a", "Task", Collections.emptyMap());
            graph.addEntity("b", "Tool", Collections.emptyMap());
            graph.addRelation("a", "b", "uses");

            List<KnowledgeGraphBuilder.RelationEdge> path = graph.queryPath("a", "b");

            assertEquals(1, path.size());
            assertEquals("uses", path.get(0).relationType());
        }

        @Test
        @DisplayName("should find indirect path")
        void shouldFindIndirectPath() {
            graph.addEntity("a", "Task", Collections.emptyMap());
            graph.addEntity("b", "Agent", Collections.emptyMap());
            graph.addEntity("c", "Tool", Collections.emptyMap());
            graph.addRelation("a", "b", "delegates_to");
            graph.addRelation("b", "c", "uses");

            List<KnowledgeGraphBuilder.RelationEdge> path = graph.queryPath("a", "c");

            assertEquals(2, path.size());
            assertEquals("delegates_to", path.get(0).relationType());
            assertEquals("uses", path.get(1).relationType());
        }

        @Test
        @DisplayName("should return empty list when no path exists")
        void shouldReturnEmptyWhenNoPath() {
            graph.addEntity("a", "Task", Collections.emptyMap());
            graph.addEntity("b", "Tool", Collections.emptyMap());

            assertThat(graph.queryPath("a", "b")).isEmpty();
        }

        @Test
        @DisplayName("should return empty list for same from and to")
        void shouldReturnEmptyForSameNode() {
            graph.addEntity("a", "Task", Collections.emptyMap());
            assertThat(graph.queryPath("a", "a")).isEmpty();
        }

        @Test
        @DisplayName("should return empty list for missing entities")
        void shouldReturnEmptyForMissingEntities() {
            assertThat(graph.queryPath("x", "y")).isEmpty();
        }

        @Test
        @DisplayName("should return empty list for null ids")
        void shouldReturnEmptyForNullIds() {
            assertThat(graph.queryPath(null, "b")).isEmpty();
            assertThat(graph.queryPath("a", null)).isEmpty();
        }
    }

    @Nested
    @DisplayName("getRelations")
    class GetRelationsTests {

        @Test
        @DisplayName("should return outgoing relations for entity")
        void shouldReturnOutgoingRelations() {
            graph.addEntity("a", "Task", Collections.emptyMap());
            graph.addEntity("b", "Tool", Collections.emptyMap());
            graph.addEntity("c", "Agent", Collections.emptyMap());
            graph.addRelation("a", "b", "uses");
            graph.addRelation("a", "c", "delegates_to");

            List<KnowledgeGraphBuilder.RelationEdge> relations = graph.getRelations("a");
            assertEquals(2, relations.size());
        }

        @Test
        @DisplayName("should return empty list for unknown entity")
        void shouldReturnEmptyForUnknownEntity() {
            assertThat(graph.getRelations("unknown")).isEmpty();
        }

        @Test
        @DisplayName("should return empty list for null id")
        void shouldReturnEmptyForNullId() {
            assertThat(graph.getRelations(null)).isEmpty();
        }
    }
}
