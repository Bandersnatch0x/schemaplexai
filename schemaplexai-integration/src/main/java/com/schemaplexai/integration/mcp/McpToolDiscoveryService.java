package com.schemaplexai.integration.mcp;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.integration.dto.McpToolSchema;
import com.schemaplexai.integration.entity.SfMcpServer;
import com.schemaplexai.integration.mapper.McpServerMapper;
import com.schemaplexai.integration.service.McpServerService;
import com.schemaplexai.integration.service.ToolExecutionService;
import com.schemaplexai.integration.tool.DiscoveredMcpToolExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Discovers tools from configured MCP servers using the real MCP protocol
 * (issue 930) and registers them into the SAME registry the execution chain
 * resolves from ({@link ToolExecutionService}).
 * <p>
 * This closes the dead-archive gap: previously discovery results were never
 * published where the execution chain could resolve them, so discovered tools
 * could never be executed. Each discovered tool is registered under the
 * qualified name {@code mcp:<serverId>:<toolName>} (SPEC-MCP format) as a
 * {@link DiscoveredMcpToolExecutor}, making it immediately queryable and
 * executable via {@code ToolExecutionService.executeTool}.
 * <p>
 * Orchestration follows SPEC-MCP: approved servers ({@code status = 'ACTIVE'}),
 * fixed daemon thread pool of 4, per-server failure isolation so one
 * unreachable server does not block the others, server tool whitelist
 * respected (null/empty ⇒ all tools allowed), already-registered tools
 * skipped, periodic schedule defaulting to 60s.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "mcp.discovery", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class McpToolDiscoveryService {

    /** Fixed thread pool size for parallel server discovery. */
    private static final int DISCOVERY_POOL_SIZE = 4;

    /** sf_mcp_server.status value that marks a server as approved (VARCHAR semantics aligned by 04dcc3a). */
    private static final String STATUS_ACTIVE = "ACTIVE";

    private final McpServerMapper mcpServerMapper;
    private final McpClientManager mcpClientManager;
    private final McpServerService mcpServerService;
    private final ToolExecutionService toolExecutionService;

    private final Executor discoveryExecutor = Executors.newFixedThreadPool(
            DISCOVERY_POOL_SIZE,
            r -> {
                Thread t = new Thread(r, "integration-mcp-discovery-" + r.hashCode());
                t.setDaemon(true);
                return t;
            });

    /**
     * Synchronously discover tools from all approved MCP servers and register
     * them into the execution-chain registry. Runs periodically; failures are
     * isolated per server (a structured {@link McpClientException} from one
     * server is logged and does not block the others).
     */
    @Scheduled(fixedDelayString = "${mcp.discovery.interval:60000}")
    public void syncAll() {
        List<SfMcpServer> servers = fetchApprovedServers();
        if (servers.isEmpty()) {
            log.debug("No approved MCP servers found, skipping discovery");
            return;
        }

        log.info("Starting MCP tool discovery for {} server(s)", servers.size());

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (SfMcpServer server : servers) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(
                    () -> discoverAndRegister(server), discoveryExecutor)
                    .exceptionally(ex -> {
                        log.warn("MCP discovery failed for server {} (endpoint: {}): {}",
                                server.getId(), server.getEndpoint(), ex.getMessage());
                        return null;
                    });
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        log.info("MCP tool discovery completed, {} tool(s) now resolvable by the execution chain",
                toolExecutionService.getRegisteredToolNames().size());
    }

    /**
     * Discover tools for a single approved server via the real MCP protocol.
     * <p>
     * This method does NOT register the tools; use {@link #syncAll()} for the
     * full chain. A structured {@link McpClientException} propagates so
     * callers can distinguish "server exposes no tools" (empty list) from
     * "server unreachable/invalid" (exception) — never a silent empty.
     *
     * @param server the approved MCP server to discover tools from
     * @return tools exposed by the server (whitelist NOT yet applied)
     */
    public List<McpToolSchema> discoverForServer(SfMcpServer server) {
        McpClient client = mcpClientManager.forServer(server);
        try {
            return client.listTools();
        } catch (BaseException e) {
            if (server.getEndpoint() != null) {
                mcpClientManager.invalidate(server.getEndpoint());
            }
            throw e;
        }
    }

    // -- internal -------------------------------------------------------

    private void discoverAndRegister(SfMcpServer server) {
        List<McpToolSchema> tools = discoverForServer(server);
        int registered = 0;
        for (McpToolSchema tool : tools) {
            if (tool.getName() == null || tool.getName().isBlank()) {
                log.warn("Skipping nameless tool discovered on server {}", server.getId());
                continue;
            }
            if (!isToolAllowed(server, tool.getName())) {
                log.debug("Tool {} skipped for server {} -- not in whitelist",
                        tool.getName(), server.getId());
                continue;
            }
            String qualifiedName = DiscoveredMcpToolExecutor.QUALIFIED_NAME_PREFIX
                    + server.getId() + ":" + tool.getName();
            if (toolExecutionService.exists(qualifiedName)) {
                log.debug("Tool {} already registered, skipping", qualifiedName);
                continue;
            }
            toolExecutionService.register(
                    new DiscoveredMcpToolExecutor(server.getId(), tool.getName(), mcpServerService));
            registered++;
            log.info("Registered MCP tool: {} (from server {} at {})",
                    qualifiedName, server.getId(), server.getEndpoint());
        }
        if (registered == 0) {
            log.debug("No new tools registered from server {} ({} discovered)",
                    server.getId(), tools.size());
        }
    }

    private List<SfMcpServer> fetchApprovedServers() {
        return mcpServerMapper.selectList(
                new LambdaQueryWrapper<SfMcpServer>()
                        .eq(SfMcpServer::getStatus, STATUS_ACTIVE));
    }

    private boolean isToolAllowed(SfMcpServer server, String toolName) {
        List<String> whitelist = server.getToolWhitelist();
        if (whitelist == null || whitelist.isEmpty()) {
            return true;
        }
        return whitelist.contains(toolName);
    }
}
