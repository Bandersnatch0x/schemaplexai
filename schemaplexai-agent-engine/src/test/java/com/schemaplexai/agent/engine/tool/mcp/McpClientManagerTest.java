package com.schemaplexai.agent.engine.tool.mcp;

import com.schemaplexai.common.exception.BaseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("McpClientManager")
class McpClientManagerTest {

    private McpClientManager manager;

    @BeforeEach
    void setUp() {
        manager = new McpClientManager();
    }

    // ── Pool configuration ──────────────────────────────────────────

    @Nested
    @DisplayName("pool configuration")
    class PoolConfigurationTests {

        @Test
        @DisplayName("should start with zero clients")
        void shouldStartWithZeroClients() {
            assertEquals(0, manager.size());
        }
    }

    // ── create() ────────────────────────────────────────────────────

    @Nested
    @DisplayName("create")
    class CreateTests {

        @Test
        @DisplayName("should create a new client for unknown endpoint")
        void shouldCreateNewClient() {
            McpClient client = manager.create("http://mcp-server-1:8080");

            assertNotNull(client);
            assertEquals("http://mcp-server-1:8080", client.getEndpoint());
            assertTrue(client.isConnected());
            assertEquals(1, manager.size());
        }

        @Test
        @DisplayName("should return cached client for same endpoint")
        void shouldReturnCachedClient() {
            McpClient first = manager.create("http://mcp-server-1:8080");
            McpClient second = manager.create("http://mcp-server-1:8080");

            assertSame(first, second, "Same endpoint must return same cached client instance");
            assertEquals(1, manager.size());
        }

        @Test
        @DisplayName("should create separate clients for different endpoints")
        void shouldCreateSeparateClientsForDifferentEndpoints() {
            McpClient client1 = manager.create("http://server-a:8080");
            McpClient client2 = manager.create("http://server-b:8080");

            assertNotSame(client1, client2);
            assertEquals(2, manager.size());
        }

        @Test
        @DisplayName("should throw BaseException when endpoint is null")
        void shouldThrowWhenEndpointIsNull() {
            BaseException ex = assertThrows(BaseException.class,
                    () -> manager.create(null));
            assertEquals(2001, ex.getCode());
        }

        @Test
        @DisplayName("should throw BaseException when endpoint is blank")
        void shouldThrowWhenEndpointIsBlank() {
            BaseException ex = assertThrows(BaseException.class,
                    () -> manager.create("  "));
            assertEquals(2001, ex.getCode());
        }
    }

    // ── get() ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("get")
    class GetTests {

        @Test
        @DisplayName("should return client when it exists in pool")
        void shouldReturnClientWhenExists() {
            manager.create("http://mcp-server-1:8080");

            McpClient client = manager.get("http://mcp-server-1:8080");

            assertNotNull(client);
            assertEquals("http://mcp-server-1:8080", client.getEndpoint());
        }

        @Test
        @DisplayName("should return null when client does not exist")
        void shouldReturnNullWhenNotExists() {
            assertNull(manager.get("http://unknown:8080"));
        }
    }

    // ── close() ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("close")
    class CloseTests {

        @Test
        @DisplayName("should evict client from pool and mark disconnected")
        void shouldEvictAndDisconnectClient() {
            manager.create("http://mcp-server-1:8080");
            assertEquals(1, manager.size());

            manager.close("http://mcp-server-1:8080");

            assertEquals(0, manager.size());
            assertNull(manager.get("http://mcp-server-1:8080"));
        }

        @Test
        @DisplayName("should be idempotent for non-existent endpoint")
        void shouldBeIdempotentForNonExistentEndpoint() {
            assertDoesNotThrow(() -> manager.close("http://unknown:8080"));
            assertEquals(0, manager.size());
        }
    }

    // ── closeAll() ──────────────────────────────────────────────────

    @Nested
    @DisplayName("closeAll")
    class CloseAllTests {

        @Test
        @DisplayName("should evict and disconnect all clients")
        void shouldEvictAndDisconnectAllClients() {
            manager.create("http://server-a:8080");
            manager.create("http://server-b:8080");
            manager.create("http://server-c:8080");
            assertEquals(3, manager.size());

            manager.closeAll();

            assertEquals(0, manager.size());
            assertNull(manager.get("http://server-a:8080"));
            assertNull(manager.get("http://server-b:8080"));
            assertNull(manager.get("http://server-c:8080"));
        }

        @Test
        @DisplayName("should be safe to call when pool is empty")
        void shouldBeSafeWhenPoolIsEmpty() {
            assertDoesNotThrow(manager::closeAll);
            assertEquals(0, manager.size());
        }
    }

    // ── preDestroy() ────────────────────────────────────────────────

    @Nested
    @DisplayName("preDestroy")
    class PreDestroyTests {

        @Test
        @DisplayName("should close all clients on destroy")
        void shouldCloseAllClientsOnDestroy() {
            manager.create("http://server-a:8080");
            manager.create("http://server-b:8080");

            manager.preDestroy();

            assertEquals(0, manager.size());
        }
    }

    // ── size() ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("size")
    class SizeTests {

        @Test
        @DisplayName("should reflect current pool size")
        void shouldReflectCurrentPoolSize() {
            assertEquals(0, manager.size());

            manager.create("http://server-a:8080");
            assertEquals(1, manager.size());

            manager.create("http://server-b:8080");
            assertEquals(2, manager.size());

            manager.close("http://server-a:8080");
            assertEquals(1, manager.size());
        }
    }

    // ── reconnection ────────────────────────────────────────────────

    @Nested
    @DisplayName("reconnection after close")
    class ReconnectionTests {

        @Test
        @DisplayName("should create fresh client after previous one was closed")
        void shouldCreateFreshClientAfterClose() {
            McpClient original = manager.create("http://mcp-server-1:8080");
            manager.close("http://mcp-server-1:8080");

            McpClient fresh = manager.create("http://mcp-server-1:8080");

            assertNotSame(original, fresh, "After close, create must return a new client instance");
            assertTrue(fresh.isConnected());
            assertEquals(1, manager.size());
        }
    }
}
