package com.schemaplexai.admin.controller;

import com.schemaplexai.admin.dto.SystemConfigDTO;
import com.schemaplexai.admin.dto.SystemConfigQuery;
import com.schemaplexai.admin.service.SystemConfigService;
import com.schemaplexai.common.result.Result;
import com.schemaplexai.model.dto.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@Tag(name = "系统配置管理")
@RestController
@RequestMapping("/admin/configs")
@RequiredArgsConstructor
public class SystemConfigController extends BaseAdminController {

    private final SystemConfigService systemConfigService;

    @Operation(summary = "分页查询系统配置")
    @GetMapping
    public Result<PageResult<SystemConfigDTO>> page(SystemConfigQuery query) {
        return success(systemConfigService.queryConfigs(query));
    }

    @Operation(summary = "获取配置详情")
    @GetMapping("/{id}")
    public Result<SystemConfigDTO> getById(@PathVariable Long id) {
        return success(systemConfigService.getConfigDetail(id));
    }

    @Operation(summary = "按Key和租户查询配置")
    @GetMapping("/by-key")
    public Result<SystemConfigDTO> getByKey(
            @RequestParam String configKey,
            @RequestParam String tenantId) {
        return success(systemConfigService.getConfigByKey(configKey, tenantId));
    }

    @Operation(summary = "创建配置")
    @PostMapping
    public Result<SystemConfigDTO> create(@RequestBody SystemConfigDTO dto) {
        return success(systemConfigService.createConfig(dto));
    }

    @Operation(summary = "更新配置")
    @PutMapping("/{id}")
    public Result<SystemConfigDTO> update(@PathVariable Long id, @RequestBody SystemConfigDTO dto) {
        return success(systemConfigService.updateConfig(id, dto));
    }

    @Operation(summary = "删除配置")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        systemConfigService.deleteConfig(id);
        return success();
    }

    @Operation(summary = "设置维护模式")
    @PostMapping("/maintenance-mode")
    public Result<Void> setMaintenanceMode(@RequestBody Map<String, Object> request) {
        boolean enabled = Boolean.TRUE.equals(request.get("enabled"));
        String tenantId = (String) request.get("tenantId");
        systemConfigService.setMaintenanceMode(enabled, tenantId);
        return success();
    }

    @Operation(summary = "设置功能开关")
    @PostMapping("/feature-flag")
    public Result<Void> setFeatureFlag(@RequestBody Map<String, Object> request) {
        String featureKey = (String) request.get("featureKey");
        boolean enabled = Boolean.TRUE.equals(request.get("enabled"));
        String tenantId = (String) request.get("tenantId");
        systemConfigService.setFeatureFlag(featureKey, enabled, tenantId);
        return success();
    }

    @Operation(summary = "检查维护模式")
    @GetMapping("/maintenance-mode")
    public Result<Map<String, Boolean>> isMaintenanceMode(@RequestParam String tenantId) {
        return success(Map.of("enabled", systemConfigService.isMaintenanceMode(tenantId)));
    }

    @Operation(summary = "检查功能开关")
    @GetMapping("/feature-flag")
    public Result<Map<String, Boolean>> isFeatureEnabled(
            @RequestParam String featureKey,
            @RequestParam String tenantId) {
        return success(Map.of("enabled", systemConfigService.isFeatureEnabled(featureKey, tenantId)));
    }
}
