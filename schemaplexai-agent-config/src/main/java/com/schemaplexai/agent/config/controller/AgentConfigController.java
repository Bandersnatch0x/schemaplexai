package com.schemaplexai.agent.config.controller;

import com.schemaplexai.agent.config.entity.SfAgent;
import com.schemaplexai.agent.config.entity.SfAgentConfig;
import com.schemaplexai.model.entity.agent.SfAgentShadowConfig;
import com.schemaplexai.agent.config.entity.SfAgentToolBinding;
import com.schemaplexai.agent.config.service.AgentConfigService;
import com.schemaplexai.agent.config.service.ShadowConfigService;
import com.schemaplexai.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/agent-config")
@RequiredArgsConstructor
public class AgentConfigController {

    private final AgentConfigService agentConfigService;
    private final ShadowConfigService shadowConfigService;

    @GetMapping("/agents")
    public Result<List<SfAgent>> listAgents() {
        return Result.success(agentConfigService.listAgents());
    }

    @GetMapping("/agents/{id}")
    public Result<SfAgent> getAgent(@PathVariable Long id) {
        return Result.success(agentConfigService.getAgent(id));
    }

    @PostMapping("/agents")
    public Result<Void> createAgent(@RequestBody SfAgent agent) {
        agentConfigService.createAgent(agent);
        return Result.success();
    }

    @PutMapping("/agents/{id}")
    public Result<Void> updateAgent(@PathVariable Long id, @RequestBody SfAgent agent) {
        agent.setId(id);
        agentConfigService.updateAgent(agent);
        return Result.success();
    }

    @DeleteMapping("/agents/{id}")
    public Result<Void> deleteAgent(@PathVariable Long id) {
        agentConfigService.deleteAgent(id);
        return Result.success();
    }

    @GetMapping("/agents/{agentId}/config")
    public Result<SfAgentConfig> getAgentConfig(@PathVariable Long agentId) {
        return Result.success(agentConfigService.getAgentConfig(agentId));
    }

    @PostMapping("/agents/{agentId}/config")
    public Result<Void> saveAgentConfig(@PathVariable Long agentId, @RequestBody SfAgentConfig config) {
        config.setAgentId(agentId);
        agentConfigService.saveAgentConfig(config);
        return Result.success();
    }

    @GetMapping("/agents/{agentId}/tools")
    public Result<List<SfAgentToolBinding>> listToolBindings(@PathVariable Long agentId) {
        return Result.success(agentConfigService.listToolBindings(agentId));
    }

    @PostMapping("/agents/{agentId}/tools")
    public Result<Void> saveToolBindings(@PathVariable Long agentId, @RequestBody List<SfAgentToolBinding> bindings) {
        agentConfigService.saveToolBindings(agentId, bindings);
        return Result.success();
    }

    @GetMapping("/shadow-configs")
    public Result<List<SfAgentShadowConfig>> listShadowConfigs() {
        return Result.success(shadowConfigService.listShadowConfigs());
    }

    @GetMapping("/shadow-configs/{agentId}")
    public Result<SfAgentShadowConfig> getShadowConfigByAgentId(@PathVariable Long agentId) {
        return Result.success(shadowConfigService.getByAgentId(agentId));
    }

    @PostMapping("/shadow-configs")
    public Result<Void> createShadowConfig(@RequestBody SfAgentShadowConfig config) {
        shadowConfigService.createShadowConfig(config);
        return Result.success();
    }

    @PutMapping("/shadow-configs/{id}")
    public Result<Void> updateShadowConfig(@PathVariable Long id, @RequestBody SfAgentShadowConfig config) {
        config.setId(id);
        shadowConfigService.updateShadowConfig(config);
        return Result.success();
    }

    @DeleteMapping("/shadow-configs/{id}")
    public Result<Void> deleteShadowConfig(@PathVariable Long id) {
        shadowConfigService.deleteShadowConfig(id);
        return Result.success();
    }
}
