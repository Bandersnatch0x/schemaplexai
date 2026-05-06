package com.schemaplexai.web.controller.config;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.schemaplexai.agent.config.service.TenantEnvironmentConfigService;
import com.schemaplexai.common.result.Result;
import com.schemaplexai.model.entity.config.TenantEnvironmentConfig;
import com.schemaplexai.web.controller.BaseController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Tenant Environment Config")
@RestController
@RequestMapping("/web/tenant-env-configs")
@RequiredArgsConstructor
public class TenantEnvironmentConfigController extends BaseController {

    private final TenantEnvironmentConfigService tenantEnvironmentConfigService;

    @Operation(summary = "Page list tenant environment configs")
    @GetMapping
    public Result<IPage<TenantEnvironmentConfig>> pageList(
            @Parameter(description = "Page number, default 1") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "Page size, default 20") @RequestParam(defaultValue = "20") Integer size) {
        IPage<TenantEnvironmentConfig> pageParam = new Page<>(page, size);
        return success(tenantEnvironmentConfigService.pageList(pageParam));
    }

    @Operation(summary = "Get tenant environment config by id")
    @GetMapping("/{id}")
    public Result<TenantEnvironmentConfig> getById(
            @Parameter(description = "Config ID") @PathVariable Long id) {
        return success(tenantEnvironmentConfigService.getById(id));
    }

    @Operation(summary = "Get tenant environment config by tenant id")
    @GetMapping("/tenant/{tenantId}")
    public Result<TenantEnvironmentConfig> getByTenantId(
            @Parameter(description = "Tenant ID") @PathVariable String tenantId) {
        return success(tenantEnvironmentConfigService.getByTenantId(tenantId));
    }

    @Operation(summary = "Create tenant environment config")
    @PostMapping
    public Result<Boolean> create(@RequestBody TenantEnvironmentConfig config) {
        return success(tenantEnvironmentConfigService.save(config));
    }

    @Operation(summary = "Update tenant environment config")
    @PutMapping("/{id}")
    public Result<Boolean> update(
            @Parameter(description = "Config ID") @PathVariable Long id,
            @RequestBody TenantEnvironmentConfig config) {
        config.setId(id);
        return success(tenantEnvironmentConfigService.updateById(config));
    }

    @Operation(summary = "Refresh tenant environment config cache")
    @PatchMapping("/{id}/refresh")
    public Result<Void> refreshCache(
            @Parameter(description = "Config ID") @PathVariable Long id) {
        TenantEnvironmentConfig config = tenantEnvironmentConfigService.getById(id);
        if (config != null && config.getTenantId() != null) {
            tenantEnvironmentConfigService.refreshCache(config.getTenantId());
        }
        return success();
    }

    @Operation(summary = "Delete tenant environment config")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(
            @Parameter(description = "Config ID") @PathVariable Long id) {
        return success(tenantEnvironmentConfigService.removeById(id));
    }
}
