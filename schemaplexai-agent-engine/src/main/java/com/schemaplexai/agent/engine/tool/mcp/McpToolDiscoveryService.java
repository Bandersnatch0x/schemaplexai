package com.schemaplexai.agent.engine.tool.mcp;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.schemaplexai.agent.engine.evaluation.ValidationResult;
import com.schemaplexai.agent.engine.guardrails.GuardrailsEngine;
import com.schemaplexai.agent.engine.tool.ToolDefinition;
import com.schemaplexai.agent.engine.tool.ToolRegistry;
import com.schemaplexai.integration.entity.SfMcpServer;
import com.schemaplexai.integration.mapper.McpServerMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Discovers tools from configured MCP servers and registers them in the {@link ToolRegistry}.
 * <p>
 * Runs periodically via Spring scheduling. Each server is discovered in parallel
 * using a fixed thread pool. Failures are isolated per-server so that one
 * unreachable server does not block discovery for the others.
 * <p>
 * Guardrails are applied to each tool description before registration.
 * Only tools that pass validation and are present in the server's whitelist
 * (or the whitelist is empty) are registered.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class McpToolDiscoveryService {

    /** Fixed thread pool size for parallel server discovery. */
    private static final int DISCOVERY_POOL_SIZE = 4;

    private final McpServerMapper mcpServerMapper;
    private final McpClientManager clientManager;
    private final ToolRegistry toolRegistry;
    private final GuardrailsEngine guardrailsEngine;

    private final Executor discoveryExecutor = Executors.newFixedThreadPool(
            DISCOVERY_POOL_SIZE,
            r -> {
                Thread t = new Thread(r, "mcp-discovery-" + r.hashCode());
                t.setDaemon(true);
                return t;
            });

    /**
     * Synchronously discover tools from all approved MCP servers and register them.
     * <p>
     * Approved servers are those with {@code status = 1} in the database.
     * Discovery runs in parallel with per-server error isolation.
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
                                server.getId(), server.getEndpoint(), ex.getMessage(), ex);
                        return null;
                    });
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        log.info("MCP tool discovery completed");
    }

    /**
     * Discover tools for a single MCP server and return the definitions.
     * <p>
     * This method does NOT register the tools in the global registry;
     * use {@link #syncAll()} for that. It is exposed primarily for testing.
     *
     * @param server the MCP server to discover tools from
     * @return list of discovered tool definitions (may be empty)
     */
    public List<ToolDefinition> discoverForServer(SfMcpServer server) {
        List<ToolDefinition> definitions = new ArrayList<>();
        try {
            McpClient client = clientManager.create(server.getEndpoint());
            if (!client.isConnected()) {
                log.warn("MCP client disconnected for server {} (endpoint: {})",
                        server.getId(), server.getEndpoint());
                return definitions;
            }

            List<McpClient.McpTool> tools = client.listTools();
            for (McpClient.McpTool tool : tools) {
                if (!isToolAllowed(server, tool.name())) {
                    log.debug("Tool {} skipped for server {} -- not in whitelist",
                            tool.name(), server.getId());
                    continue;
                }

                ValidationResult guardrailsResult = guardrailsEngine.validateInput(tool.description());
                if (!guardrailsResult.success()) {
                    log.warn("Tool {} on server {} failed guardrails: {}",
                            tool.name(), server.getId(), guardrailsResult.errorMessage());
                    continue;
                }

                String qualifiedName = buildQualifiedName(server, tool.name());
                if (toolRegistry.exists(qualifiedName)) {
                    log.debug("Tool {} already registered, skipping", qualifiedName);
                    continue;
                }

                ToolDefinition definition = new ToolDefinition(
                        qualifiedName,
                        tool.description(),
                        List.of(), // parameters unknown at discovery time
                        "object"   // generic return type
                );
                definitions.add(definition);
            }
        } catch (Exception ex) {
            log.warn("Discovery failed for server {} (endpoint: {}): {}",
                    server.getId(), server.getEndpoint(), ex.getMessage(), ex);
        }
        return definitions;
    }

    // -- internal -------------------------------------------------------

    private void discoverAndRegister(SfMcpServer server) {
        List<ToolDefinition> definitions = discoverForServer(server);
        for (ToolDefinition definition : definitions) {
            toolRegistry.register(definition);
            log.info("Registered MCP tool: {} (from server {})",
                    definition.name(), server.getId());
        }
    }

    private List<SfMcpServer> fetchApprovedServers() {
        return mcpServerMapper.selectList(
                new LambdaQueryWrapper<SfMcpServer>()
                        .eq(SfMcpServer::getStatus, 1)
        );
    }

    private boolean isToolAllowed(SfMcpServer server, String toolName) {
        List<String> whitelist = server.getToolWhitelist();
        if (whitelist == null || whitelist.isEmpty()) {
            return true;
        }
        return whitelist.contains(toolName);
    }

    private String buildQualifiedName(SfMcpServer server, String toolName) {
        return "mcp:" + server.getId() + ":" + toolName;
    }
}
