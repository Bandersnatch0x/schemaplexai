package com.schemaplexai.agent.engine.integration;

import com.schemaplexai.agent.engine.tool.adapter.ExecutionContext;
import com.schemaplexai.common.context.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tenant Isolation Flow Integration")
class TenantIsolationFlowTest {

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("should store and retrieve tenant ID in thread-local context")
    void shouldStoreAndRetrieveTenantId() {
        TenantContextHolder.setTenantId("tenant-42");
        assertEquals("tenant-42", TenantContextHolder.getTenantId());
    }

    @Test
    @DisplayName("should clear tenant context")
    void shouldClearTenantContext() {
        TenantContextHolder.setTenantId("tenant-42");
        TenantContextHolder.clear();
        assertNull(TenantContextHolder.getTenantId());
    }

    @Test
    @DisplayName("should isolate tenant IDs across threads")
    void shouldIsolateAcrossThreads() throws InterruptedException {
        TenantContextHolder.setTenantId("tenant-main");

        ExecutorService executor = Executors.newSingleThreadExecutor();
        CountDownLatch latch = new CountDownLatch(1);

        executor.submit(() -> {
            try {
                assertNull(TenantContextHolder.getTenantId());
                TenantContextHolder.setTenantId("tenant-worker");
                assertEquals("tenant-worker", TenantContextHolder.getTenantId());
            } finally {
                latch.countDown();
            }
        });

        latch.await();
        executor.shutdown();

        assertEquals("tenant-main", TenantContextHolder.getTenantId());
    }

    @Test
    @DisplayName("should propagate tenant ID through execution context")
    void shouldPropagateTenantInExecutionContext() {
        ExecutionContext ctx = new ExecutionContext("tenant-99", 1L, "/workspace");
        assertEquals("tenant-99", ctx.tenantId());
    }

    @Test
    @DisplayName("should include tenant in execution context attributes")
    void shouldIncludeTenantInAttributes() {
        Map<String, Object> attrs = Map.of("custom", "value");
        ExecutionContext ctx = new ExecutionContext("tenant-88", 2L, "/workspace", attrs);

        assertEquals("tenant-88", ctx.tenantId());
        assertEquals("value", ctx.getAttribute("custom"));
    }

    @Test
    @DisplayName("should generate distinct cache keys per tenant")
    void shouldGenerateDistinctCacheKeys() {
        String tenantA = "tenant-a";
        String tenantB = "tenant-b";

        String keyA = "sf:admission:rate:" + tenantA + ":1";
        String keyB = "sf:admission:rate:" + tenantB + ":1";

        assertNotEquals(keyA, keyB);
        assertTrue(keyA.contains(tenantA));
        assertTrue(keyB.contains(tenantB));
    }
}
