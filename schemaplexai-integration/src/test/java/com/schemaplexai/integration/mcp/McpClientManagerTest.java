package com.schemaplexai.integration.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.integration.entity.SfMcpServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link McpClientManager} (issue 930): clients are cached per
 * configured endpoint, invalidated after structured failures, and never
 * derived from the numeric server id.
 */
class McpClientManagerTest {

    private McpClientManager manager;

    @BeforeEach
    void setUp() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(30));
        factory.setReadTimeout(Duration.ofSeconds(30));
        manager = new McpClientManager(new RestTemplate(factory), new ObjectMapper());
    }

    private static SfMcpServer server(Long id, String endpoint) {
        SfMcpServer server = new SfMcpServer();
        server.setId(id);
        server.setEndpoint(endpoint);
        return server;
    }

    @Test
    void forServer_cachesClientPerEndpoint() {
        McpClient first = manager.forServer(server(1L, "http://a:9000"));
        McpClient second = manager.forServer(server(1L, "http://a:9000"));
        McpClient other = manager.forServer(server(2L, "http://b:9000"));

        assertThat(first).isSameAs(second);
        assertThat(other).isNotSameAs(first);
        assertThat(manager.size()).isEqualTo(2);
    }

    @Test
    void forServer_usesConfiguredEndpoint_notTheId() {
        McpClient client = manager.forServer(server(42L, "http://real-endpoint:9000"));

        assertThat(client.getEndpoint()).isEqualTo("http://real-endpoint:9000");
    }

    @Test
    void forServer_nullServer_throwsNotFound() {
        assertThatThrownBy(() -> manager.forServer(null))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.INTEGRATION_NOT_FOUND.getCode());
    }

    @Test
    void forServer_missingEndpoint_throwsNotFound() {
        assertThatThrownBy(() -> manager.forServer(server(1L, null)))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.INTEGRATION_NOT_FOUND.getCode());
        assertThatThrownBy(() -> manager.forServer(server(1L, "  ")))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.INTEGRATION_NOT_FOUND.getCode());
    }

    @Test
    void invalidate_dropsClientSoNextCallReinitializes() {
        McpClient before = manager.forServer(server(1L, "http://a:9000"));
        manager.invalidate("http://a:9000");
        McpClient after = manager.forServer(server(1L, "http://a:9000"));

        assertThat(after).isNotSameAs(before);
        assertThat(manager.size()).isEqualTo(1);
    }

    @Test
    void invalidate_nullOrUnknownEndpoint_isIdempotent() {
        manager.forServer(server(1L, "http://a:9000"));
        manager.invalidate(null);
        manager.invalidate("http://unknown:9000");

        assertThat(manager.size()).isEqualTo(1);
    }
}
