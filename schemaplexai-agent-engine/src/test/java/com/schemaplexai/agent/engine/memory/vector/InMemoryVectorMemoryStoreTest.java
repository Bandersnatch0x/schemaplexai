package com.schemaplexai.agent.engine.memory.vector;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class InMemoryVectorMemoryStoreTest {

    private InMemoryVectorMemoryStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryVectorMemoryStore();
    }

    @Test
    void storeAndRetrieveByKeyword() {
        MemoryFragment fragment = createFragment("agent-1", "tenant-1", "Spring Boot configuration guide");
        store.store(fragment);

        List<MemoryFragment> results = store.retrieve("agent-1", "tenant-1", "spring", 10);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).content()).isEqualTo("Spring Boot configuration guide");
    }

    @Test
    void retrieveReturnsEmptyForNonMatchingQuery() {
        store.store(createFragment("agent-1", "tenant-1", "Spring Boot configuration guide"));

        List<MemoryFragment> results = store.retrieve("agent-1", "tenant-1", "kubernetes", 10);

        assertThat(results).isEmpty();
    }

    @Test
    void retrieveReturnsEmptyForBlankQuery() {
        store.store(createFragment("agent-1", "tenant-1", "some content"));

        assertThat(store.retrieve("agent-1", "tenant-1", "", 10)).isEmpty();
        assertThat(store.retrieve("agent-1", "tenant-1", "   ", 10)).isEmpty();
        assertThat(store.retrieve("agent-1", "tenant-1", null, 10)).isEmpty();
    }

    @Test
    void retrieveIsCaseInsensitive() {
        store.store(createFragment("agent-1", "tenant-1", "Java Spring Boot"));

        List<MemoryFragment> results = store.retrieve("agent-1", "tenant-1", "JAVA", 10);

        assertThat(results).hasSize(1);
    }

    @Test
    void retrieveLimitsResults() {
        for (int i = 0; i < 10; i++) {
            store.store(createFragment("agent-1", "tenant-1", "item " + i + " with keyword"));
        }

        List<MemoryFragment> results = store.retrieve("agent-1", "tenant-1", "keyword", 3);

        assertThat(results).hasSize(3);
    }

    @Test
    void retrieveSortsByImportanceDescending() {
        store.store(createFragment("agent-1", "tenant-1", "low importance keyword", 0.2));
        store.store(createFragment("agent-1", "tenant-1", "high importance keyword", 0.9));
        store.store(createFragment("agent-1", "tenant-1", "medium importance keyword", 0.5));

        List<MemoryFragment> results = store.retrieve("agent-1", "tenant-1", "keyword", 10);

        assertThat(results).hasSize(3);
        assertThat(results.get(0).importance()).isEqualTo(0.9);
        assertThat(results.get(1).importance()).isEqualTo(0.5);
        assertThat(results.get(2).importance()).isEqualTo(0.2);
    }

    @Test
    void retrieveIsScopedByAgentAndTenant() {
        store.store(createFragment("agent-1", "tenant-1", "secret keyword data"));
        store.store(createFragment("agent-2", "tenant-1", "other keyword data"));
        store.store(createFragment("agent-1", "tenant-2", "another keyword data"));

        List<MemoryFragment> results = store.retrieve("agent-1", "tenant-1", "keyword", 10);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).content()).isEqualTo("secret keyword data");
    }

    @Test
    void forgetRemovesOldMemories() {
        Instant now = Instant.now();
        Instant oneHourAgo = now.minusSeconds(3600);
        Instant twoHoursAgo = now.minusSeconds(7200);

        store.store(createFragmentWithTime("agent-1", "tenant-1", "old memory", twoHoursAgo));
        store.store(createFragmentWithTime("agent-1", "tenant-1", "recent memory", now));

        store.forget("agent-1", "tenant-1", oneHourAgo);

        List<MemoryFragment> remaining = store.listRecent("agent-1", "tenant-1", 10);
        assertThat(remaining).hasSize(1);
        assertThat(remaining.get(0).content()).isEqualTo("recent memory");
    }

    @Test
    void forgetWithNullBeforeDoesNothing() {
        store.store(createFragment("agent-1", "tenant-1", "some memory"));

        store.forget("agent-1", "tenant-1", null);

        assertThat(store.listRecent("agent-1", "tenant-1", 10)).hasSize(1);
    }

    @Test
    void forgetOnlyAffectsSpecifiedAgentAndTenant() {
        Instant now = Instant.now();
        Instant twoHoursAgo = now.minusSeconds(7200);

        store.store(createFragmentWithTime("agent-1", "tenant-1", "memory a1t1", twoHoursAgo));
        store.store(createFragmentWithTime("agent-2", "tenant-1", "memory a2t1", twoHoursAgo));

        store.forget("agent-1", "tenant-1", now);

        assertThat(store.listRecent("agent-1", "tenant-1", 10)).isEmpty();
        assertThat(store.listRecent("agent-2", "tenant-1", 10)).hasSize(1);
    }

    @Test
    void listRecentReturnsMostRecentFirst() {
        Instant now = Instant.now();
        store.store(createFragmentWithTime("agent-1", "tenant-1", "first", now.minusSeconds(300)));
        store.store(createFragmentWithTime("agent-1", "tenant-1", "second", now.minusSeconds(200)));
        store.store(createFragmentWithTime("agent-1", "tenant-1", "third", now.minusSeconds(100)));

        List<MemoryFragment> results = store.listRecent("agent-1", "tenant-1", 10);

        assertThat(results).hasSize(3);
        assertThat(results.get(0).content()).isEqualTo("third");
        assertThat(results.get(1).content()).isEqualTo("second");
        assertThat(results.get(2).content()).isEqualTo("first");
    }

    @Test
    void listRecentRespectsLimit() {
        for (int i = 0; i < 10; i++) {
            store.store(createFragment("agent-1", "tenant-1", "memory " + i));
        }

        List<MemoryFragment> results = store.listRecent("agent-1", "tenant-1", 3);

        assertThat(results).hasSize(3);
    }

    @Test
    void listRecentReturnsEmptyForUnknownAgent() {
        store.store(createFragment("agent-1", "tenant-1", "memory"));

        List<MemoryFragment> results = store.listRecent("agent-unknown", "tenant-1", 10);

        assertThat(results).isEmpty();
    }

    @Test
    void storeThrowsOnNullFragment() {
        assertThatThrownBy(() -> store.store(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be null");
    }

    @Test
    void storeThrowsOnBlankAgentId() {
        MemoryFragment fragment = new MemoryFragment("id", "", "tenant-1", "content", "test", 0.5, Instant.now(), Map.of());
        assertThatThrownBy(() -> store.store(fragment))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("agentId");
    }

    @Test
    void storeThrowsOnBlankTenantId() {
        MemoryFragment fragment = new MemoryFragment("id", "agent-1", "", "content", "test", 0.5, Instant.now(), Map.of());
        assertThatThrownBy(() -> store.store(fragment))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tenantId");
    }

    @Test
    void storeThrowsOnBlankContent() {
        MemoryFragment fragment = new MemoryFragment("id", "agent-1", "tenant-1", "", "test", 0.5, Instant.now(), Map.of());
        assertThatThrownBy(() -> store.store(fragment))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("content");
    }

    @Test
    void retrieveReturnsEmptyForEmptyStore() {
        List<MemoryFragment> results = store.retrieve("agent-1", "tenant-1", "anything", 10);

        assertThat(results).isEmpty();
    }

    // --- helpers ---

    private MemoryFragment createFragment(String agentId, String tenantId, String content) {
        return new MemoryFragment(
                java.util.UUID.randomUUID().toString(),
                agentId, tenantId, content, "test", 0.5, Instant.now(), new HashMap<>()
        );
    }

    private MemoryFragment createFragment(String agentId, String tenantId, String content, double importance) {
        return new MemoryFragment(
                java.util.UUID.randomUUID().toString(),
                agentId, tenantId, content, "test", importance, Instant.now(), new HashMap<>()
        );
    }

    private MemoryFragment createFragmentWithTime(String agentId, String tenantId, String content, Instant createdAt) {
        return new MemoryFragment(
                java.util.UUID.randomUUID().toString(),
                agentId, tenantId, content, "test", 0.5, createdAt, new HashMap<>()
        );
    }
}
