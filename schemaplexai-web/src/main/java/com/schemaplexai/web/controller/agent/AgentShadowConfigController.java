package com.schemaplexai.web.controller.agent;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.schemaplexai.agent.config.service.AgentShadowConfigService;
import com.schemaplexai.common.result.Result;
import com.schemaplexai.model.entity.agent.SfAgentShadowConfig;
import com.schemaplexai.web.controller.BaseController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Agent Shadow Config")
@RestController
@RequestMapping("/agent-config/shadow-configs")
@RequiredArgsConstructor
public class AgentShadowConfigController extends BaseController {

    private final AgentShadowConfigService agentShadowConfigService;

    @Operation(summary = "Page list shadow configs")
    @GetMapping
    public Result<IPage<SfAgentShadowConfig>> pageList(
            @Parameter(description = "Page number, default 1") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "Page size, default 20") @RequestParam(defaultValue = "20") Integer size) {
        IPage<SfAgentShadowConfig> pageParam = new Page<>(page, size);
        return success(agentShadowConfigService.pageList(pageParam));
    }

    @Operation(summary = "Get shadow config by id")
    @GetMapping("/{id}")
    public Result<SfAgentShadowConfig> getById(
            @Parameter(description = "Config ID") @PathVariable Long id) {
        return success(agentShadowConfigService.getById(id));
    }

    @Operation(summary = "Get shadow config by agent id")
    @GetMapping("/agent/{agentId}")
    public Result<SfAgentShadowConfig> getByAgentId(
            @Parameter(description = "Agent ID") @PathVariable Long agentId) {
        return success(agentShadowConfigService.getByAgentId(agentId));
    }

    @Operation(summary = "Create shadow config")
    @PostMapping
    public Result<Boolean> create(@RequestBody SfAgentShadowConfig config) {
        return success(agentShadowConfigService.save(config));
    }

    @Operation(summary = "Update shadow config")
    @PutMapping("/{id}")
    public Result<Boolean> update(
            @Parameter(description = "Config ID") @PathVariable Long id,
            @RequestBody SfAgentShadowConfig config) {
        config.setId(id);
        return success(agentShadowConfigService.updateById(config));
    }

    @Operation(summary = "Toggle enabled status")
    @PatchMapping("/{id}/toggle")
    public Result<Void> toggleEnabled(
            @Parameter(description = "Config ID") @PathVariable Long id,
            @RequestParam boolean enabled) {
        agentShadowConfigService.toggleEnabled(id, enabled);
        return success();
    }

    @Operation(summary = "Delete shadow config")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(
            @Parameter(description = "Config ID") @PathVariable Long id) {
        return success(agentShadowConfigService.removeById(id));
    }
}
