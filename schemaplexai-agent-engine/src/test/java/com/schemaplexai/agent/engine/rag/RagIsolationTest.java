package com.schemaplexai.agent.engine.rag;

import com.schemaplexai.agent.engine.context.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RagIsolationTest {

    // ── RagIsolationConfig tests ────────────────────────────────

    @Nested
    @DisplayName("RagIsolationConfig")
    class ConfigTests {

        private RagIsolationConfig config;

        @BeforeEach
        void setUp() {
            config = RagIsolationConfig.defaultConfig();
        }

        @Test
        @DisplayName("defaultConfig() has expected defaults")
        void defaultConfigHasExpectedValues() {
            assertEquals("tenant_", config.collectionPrefix());
            assertTrue(config.enablePartitionIsolation());
            assertTrue(config.enableSearchFilter());
            assertEquals(10, config.maxResults());
            assertEquals(0.7, config.similarityThreshold());
        }

        @Test
        @DisplayName("collectionName() prepends tenant prefix")
        void collectionNamePrependsPrefix() {
            assertEquals("tenant_tenant123", config.collectionName("tenant123"));
            assertEquals("tenant_acme", config.collectionName("acme"));
        }

        @Test
        @DisplayName("searchFilter() includes tenant_id only when project is null")
        void searchFilterTenantOnlyWhenProjectNull() {
            String filter = config.searchFilter("tenant1", null);
            assertEquals("tenant_id == 'tenant1'", filter);
        }

        @Test
        @DisplayName("searchFilter() includes tenant_id only when project is blank")
        void searchFilterTenantOnlyWhenProjectBlank() {
            String filter = config.searchFilter("tenant1", "");
            assertEquals("tenant_id == 'tenant1'", filter);
        }

        @Test
        @DisplayName("searchFilter() includes both tenant_id and project_id")
        void searchFilterIncludesBoth() {
            String filter = config.searchFilter("tenant1", "project1");
            assertEquals("tenant_id == 'tenant1' AND project_id == 'project1'", filter);
        }

        @Test
        @DisplayName("custom config overrides defaults")
        void customConfig() {
            RagIsolationConfig custom = new RagIsolationConfig(
                    "proj_", false, false, 20, 0.5
            );
            assertEquals("proj_", custom.collectionPrefix());
            assertFalse(custom.enablePartitionIsolation());
            assertFalse(custom.enableSearchFilter());
            assertEquals(20, custom.maxResults());
            assertEquals(0.5, custom.similarityThreshold());
            assertEquals("proj_tenantX", custom.collectionName("tenantX"));
        }
    }

    // ── MilvusIsolationService tests ────────────────────────────

    @Nested
    @DisplayName("MilvusIsolationService")
    class ServiceTests {

        private MilvusIsolationService ragService;
        private RagIsolationConfig config;
        private StubMilvusClient stubClient;

        @BeforeEach
        void setUp() {
            config = RagIsolationConfig.defaultConfig();
            stubClient = new StubMilvusClient();
            stubClient.addCollection("tenant_tenant1");
            stubClient.addSearchResult("tenant_tenant1",
                    new SearchResult("Java is a programming language.", "wiki/java.md", 0.92));

            ragService = new MilvusIsolationService(config, stubClient);
        }

        // -- validateSearchPermission --

        @Test
        @DisplayName("rejects search with null tenant ID")
        void rejectsNullTenant() {
            ValidationResult result = ragService.validateSearchPermission(null, "project1");
            assertFalse(result.isValid());
            assertTrue(result.getErrorMessage().contains("Tenant ID"));
        }

        @Test
        @DisplayName("rejects search with blank tenant ID")
        void rejectsBlankTenant() {
            ValidationResult result = ragService.validateSearchPermission("  ", "project1");
            assertFalse(result.isValid());
        }

        @Test
        @DisplayName("rejects search for non-existent collection")
        void rejectsNonExistentCollection() {
            ValidationResult result = ragService.validateSearchPermission("nonexistent", "project1");
            assertFalse(result.isValid());
            assertTrue(result.getErrorMessage().contains("Collection not found"));
        }

        @Test
        @DisplayName("accepts valid tenant with existing collection")
        void acceptsValidTenant() {
            ValidationResult result = ragService.validateSearchPermission("tenant1", "project1");
            assertTrue(result.isValid());
            assertNull(result.getErrorMessage());
        }

        @Test
        @DisplayName("accepts valid tenant with null project")
        void acceptsValidTenantWithNullProject() {
            ValidationResult result = ragService.validateSearchPermission("tenant1", null);
            assertTrue(result.isValid());
        }

        // -- searchWithIsolation --

        @Test
        @DisplayName("throws SecurityException when tenant is null")
        void throwsOnNullTenant() {
            float[] embedding = {0.1f, 0.2f, 0.3f};
            assertThrows(SecurityException.class, () ->
                    ragService.searchWithIsolation(null, "project1", embedding));
        }

        @Test
        @DisplayName("throws SecurityException when collection does not exist")
        void throwsOnMissingCollection() {
            float[] embedding = {0.1f, 0.2f, 0.3f};
            assertThrows(SecurityException.class, () ->
                    ragService.searchWithIsolation("unknown_tenant", "project1", embedding));
        }

        @Test
        @DisplayName("returns results for valid tenant")
        void returnsResultsForValidTenant() {
            float[] embedding = {0.1f, 0.2f, 0.3f};
            List<SearchResult> results = ragService.searchWithIsolation("tenant1", "project1", embedding);

            assertNotNull(results);
            assertEquals(1, results.size());
            assertEquals("Java is a programming language.", results.get(0).getContent());
        }

        @Test
        @DisplayName("passes correct collection name and filter to client")
        void passesCorrectParamsToClient() {
            float[] embedding = {0.5f};
            ragService.searchWithIsolation("tenant1", "projA", embedding);

            assertEquals("tenant_tenant1", stubClient.getLastSearchCollection());
            assertEquals("tenant_id == 'tenant1' AND project_id == 'projA'",
                    stubClient.getLastSearchFilter());
        }

        @Test
        @DisplayName("skips filter when enableSearchFilter is false")
        void skipsFilterWhenDisabled() {
            RagIsolationConfig noFilterConfig = new RagIsolationConfig(
                    "tenant_", true, false, 10, 0.7
            );
            MilvusIsolationService service = new MilvusIsolationService(noFilterConfig, stubClient);
            float[] embedding = {0.5f};
            service.searchWithIsolation("tenant1", "projA", embedding);

            assertNull(stubClient.getLastSearchFilter());
        }

        @Test
        @DisplayName("skips collection check when partition isolation disabled")
        void skipsCollectionCheckWhenPartitionDisabled() {
            RagIsolationConfig noPartition = new RagIsolationConfig(
                    "tenant_", false, true, 10, 0.7
            );
            MilvusIsolationService service = new MilvusIsolationService(noPartition, stubClient);

            // Tenant that has no collection should still be valid when partition isolation is off
            ValidationResult result = service.validateSearchPermission("unknown", "p1");
            assertTrue(result.isValid());
        }
    }

    // ── Stub MilvusClient for testing ───────────────────────────

    private static class StubMilvusClient implements MilvusClient {

        private final Map<String, List<SearchResult>> collections = new HashMap<>();
        private String lastSearchCollection;
        private String lastSearchFilter;

        void addCollection(String name) {
            collections.put(name, new ArrayList<>());
        }

        void addSearchResult(String collection, SearchResult result) {
            collections.computeIfAbsent(collection, k -> new ArrayList<>()).add(result);
        }

        @Override
        public boolean hasCollection(String collectionName) {
            return collections.containsKey(collectionName);
        }

        @Override
        public List<SearchResult> search(String collectionName, float[] queryEmbedding,
                                          int maxResults, double similarityThreshold, String filter) {
            lastSearchCollection = collectionName;
            lastSearchFilter = filter;
            return collections.getOrDefault(collectionName, List.of());
        }

        String getLastSearchCollection() {
            return lastSearchCollection;
        }

        String getLastSearchFilter() {
            return lastSearchFilter;
        }
    }
}
