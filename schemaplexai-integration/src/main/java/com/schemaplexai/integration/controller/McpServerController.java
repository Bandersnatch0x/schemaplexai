package com.schemaplexai.integration.controller;

import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.integration.entity.SfMcpServer;
import com.schemaplexai.integration.service.McpServerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/integration/mcp-servers")
@RequiredArgsConstructor
public class McpServerController {

    private final McpServerService mcpServerService;

    @PostMapping
    public Result<Long> create(@RequestBody SfMcpServer mcpServer) {
        mcpServerService.save(mcpServer);
        return Result.success(mcpServer.getId());
    }

    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody SfMcpServer mcpServer) {
        mcpServer.setId(id);
        return Result.success(mcpServerService.updateById(mcpServer));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(mcpServerService.removeById(id));
    }

    @GetMapping("/{id}")
    public Result<SfMcpServer> get(@PathVariable Long id) {
        SfMcpServer mcpServer = mcpServerService.getById(id);
        if (mcpServer == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        return Result.success(mcpServer);
    }

    @GetMapping
    public Result<List<SfMcpServer>> list() {
        return Result.success(mcpServerService.list());
    }
}
