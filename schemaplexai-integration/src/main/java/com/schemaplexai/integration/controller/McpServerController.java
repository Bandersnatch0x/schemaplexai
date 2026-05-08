package com.schemaplexai.integration.controller;

import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.integration.dto.McpToolSchema;
import com.schemaplexai.integration.entity.SfMcpServer;
import com.schemaplexai.integration.service.McpServerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/integration/mcp-servers")
@RequiredArgsConstructor
@Tag(name = "MCP Server Management", description = "Model Context Protocol server registration and tool invocation")
public class McpServerController {

    private final McpServerService mcpServerService;

    @PostMapping
    @Operation(summary = "Register MCP server")
    public Result<Long> create(@RequestBody SfMcpServer mcpServer) {
        mcpServerService.save(mcpServer);
        return Result.success(mcpServer.getId());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update MCP server")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody SfMcpServer mcpServer) {
        mcpServer.setId(id);
        return Result.success(mcpServerService.updateById(mcpServer));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete MCP server")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(mcpServerService.removeById(id));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get MCP server by id")
    public Result<SfMcpServer> get(@PathVariable Long id) {
        SfMcpServer mcpServer = mcpServerService.getById(id);
        if (mcpServer == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        return Result.success(mcpServer);
    }

    @GetMapping
    @Operation(summary = "List all MCP servers")
    public Result<List<SfMcpServer>> list() {
        return Result.success(mcpServerService.list());
    }

    @PostMapping("/{id}/discover")
    @Operation(summary = "Discover tools from MCP server")
    public Result<List<McpToolSchema>> discoverTools(@PathVariable Long id) {
        return Result.success(mcpServerService.discoverTools(id));
    }

    @PostMapping("/{id}/invoke")
    @Operation(summary = "Invoke tool on MCP server")
    public Result<String> invokeTool(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String toolName = (String) body.get("toolName");
        @SuppressWarnings("unchecked")
        Map<String, Object> arguments = (Map<String, Object>) body.getOrDefault("arguments", Map.of());
        return Result.success(mcpServerService.invokeTool(id, toolName, arguments));
    }
}
