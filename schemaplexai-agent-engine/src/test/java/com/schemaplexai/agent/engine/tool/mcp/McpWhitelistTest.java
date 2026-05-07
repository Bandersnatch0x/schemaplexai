package com.schemaplexai.agent.engine.tool.mcp;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.schemaplexai.integration.entity.SfMcpServer;
import com.schemaplexai.integration.mapper.McpServerMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("McpServerRegistry - whitelist checks")
class McpWhitelistTest {

    private static final String SERVER_ENDPOINT = "https://mcp.example.com/api";
    private static final Long TENANT_ID = 1L;

    @Mock
    private McpServerMapper mcpServerMapper;

    @InjectMocks
    private McpServerRegistry registry;

    private SfMcpServer activeServer;

    @BeforeEach
    void setUp() {
        activeServer = new SfMcpServer();
        activeServer.setId(100L);
        activeServer.setEndpoint(SERVER_ENDPOINT);
        activeServer.setStatus(1);
        activeServer.setTenantId(String.valueOf(TENANT_ID));
    }

    // --- isAllowed ---

    @Nested
    @DisplayName("isAllowed")
    class IsAllowedTests {

        @Test
        @DisplayName("should return true when server has status=1 in DB")
        void shouldAllowActiveServer() {
            when(mcpServerMapper.selectOne(any(LambdaQueryWrapper.class)))
                    .thenReturn(activeServer);

            boolean result = registry.isAllowed(SERVER_ENDPOINT, TENANT_ID);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false when server is not in DB")
        void shouldRejectUnknownServer() {
            when(mcpServerMapper.selectOne(any(LambdaQueryWrapper.class)))
                    .thenReturn(null);

            boolean result = registry.isAllowed(SERVER_ENDPOINT, TENANT_ID);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false when server status is 0 (disabled)")
        void shouldRejectDisabledServer() {
            activeServer.setStatus(0);
            when(mcpServerMapper.selectOne(any(LambdaQueryWrapper.class)))
                    .thenReturn(activeServer);

            boolean result = registry.isAllowed(SERVER_ENDPOINT, TENANT_ID);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false when server status is 2 (suspended)")
        void shouldRejectSuspendedServer() {
            activeServer.setStatus(2);
            when(mcpServerMapper.selectOne(any(LambdaQueryWrapper.class)))
                    .thenReturn(activeServer);

            boolean result = registry.isAllowed(SERVER_ENDPOINT, TENANT_ID);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false when server status is null")
        void shouldRejectServerWithNullStatus() {
            activeServer.setStatus(null);
            when(mcpServerMapper.selectOne(any(LambdaQueryWrapper.class)))
                    .thenReturn(activeServer);

            boolean result = registry.isAllowed(SERVER_ENDPOINT, TENANT_ID);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false when endpoint is null")
        void shouldRejectNullEndpoint() {
            boolean result = registry.isAllowed(null, TENANT_ID);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false when endpoint is blank")
        void shouldRejectBlankEndpoint() {
            boolean result = registry.isAllowed("   ", TENANT_ID);

            assertThat(result).isFalse();
        }
    }

    // --- isToolAllowed ---

    @Nested
    @DisplayName("isToolAllowed")
    class IsToolAllowedTests {

        @Test
        @DisplayName("should return true when tool is in server toolWhitelist")
        void shouldAllowListedTool() {
            activeServer.setToolWhitelist(List.of("read_file", "write_file"));
            when(mcpServerMapper.selectOne(any(LambdaQueryWrapper.class)))
                    .thenReturn(activeServer);

            boolean result = registry.isToolAllowed(SERVER_ENDPOINT, "read_file", TENANT_ID);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false when tool is not in server toolWhitelist")
        void shouldRejectUnlistedTool() {
            activeServer.setToolWhitelist(List.of("read_file", "write_file"));
            when(mcpServerMapper.selectOne(any(LambdaQueryWrapper.class)))
                    .thenReturn(activeServer);

            boolean result = registry.isToolAllowed(SERVER_ENDPOINT, "delete_file", TENANT_ID);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return true when server has no toolWhitelist restriction (null)")
        void shouldAllowAllToolsWhenWhitelistIsNull() {
            activeServer.setToolWhitelist(null);
            when(mcpServerMapper.selectOne(any(LambdaQueryWrapper.class)))
                    .thenReturn(activeServer);

            boolean result = registry.isToolAllowed(SERVER_ENDPOINT, "any_tool", TENANT_ID);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return true when server has empty toolWhitelist")
        void shouldAllowAllToolsWhenWhitelistIsEmpty() {
            activeServer.setToolWhitelist(List.of());
            when(mcpServerMapper.selectOne(any(LambdaQueryWrapper.class)))
                    .thenReturn(activeServer);

            boolean result = registry.isToolAllowed(SERVER_ENDPOINT, "any_tool", TENANT_ID);

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("should return false when server is not active")
        void shouldRejectToolOnInactiveServer() {
            activeServer.setStatus(0);
            when(mcpServerMapper.selectOne(any(LambdaQueryWrapper.class)))
                    .thenReturn(activeServer);

            boolean result = registry.isToolAllowed(SERVER_ENDPOINT, "read_file", TENANT_ID);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("should return false when server not found")
        void shouldRejectToolOnUnknownServer() {
            when(mcpServerMapper.selectOne(any(LambdaQueryWrapper.class)))
                    .thenReturn(null);

            boolean result = registry.isToolAllowed(SERVER_ENDPOINT, "read_file", TENANT_ID);

            assertThat(result).isFalse();
        }
    }

    // --- getServer ---

    @Nested
    @DisplayName("getServer")
    class GetServerTests {

        @Test
        @DisplayName("should return server when found and active")
        void shouldReturnActiveServer() {
            when(mcpServerMapper.selectOne(any(LambdaQueryWrapper.class)))
                    .thenReturn(activeServer);

            SfMcpServer result = registry.getServer(SERVER_ENDPOINT, TENANT_ID);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(100L);
            assertThat(result.getEndpoint()).isEqualTo(SERVER_ENDPOINT);
        }

        @Test
        @DisplayName("should return null when server not found")
        void shouldReturnNullWhenNotFound() {
            when(mcpServerMapper.selectOne(any(LambdaQueryWrapper.class)))
                    .thenReturn(null);

            SfMcpServer result = registry.getServer(SERVER_ENDPOINT, TENANT_ID);

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("should return null when server is inactive")
        void shouldReturnNullWhenInactive() {
            activeServer.setStatus(0);
            when(mcpServerMapper.selectOne(any(LambdaQueryWrapper.class)))
                    .thenReturn(activeServer);

            SfMcpServer result = registry.getServer(SERVER_ENDPOINT, TENANT_ID);

            assertThat(result).isNull();
        }
    }
}
