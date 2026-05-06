package com.schemaplexai.admin.controller;

import com.schemaplexai.admin.dto.TenantAdminDTO;
import com.schemaplexai.admin.dto.TenantConfigUpdateDTO;
import com.schemaplexai.admin.service.TenantAdminService;
import com.schemaplexai.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "租户管理（管理端）")
@RestController
@RequestMapping("/admin/tenants")
@RequiredArgsConstructor
public class TenantAdminController extends BaseAdminController {

    private final TenantAdminService tenantAdminService;

    @Operation(summary = "获取所有租户详情（含统计）")
    @GetMapping
    public Result<List<TenantAdminDTO>> listAll() {
        return success(tenantAdminService.listAllTenantDetails());
    }

    @Operation(summary = "获取单个租户管理详情")
    @GetMapping("/{id}")
    public Result<TenantAdminDTO> getDetail(@PathVariable Long id) {
        return success(tenantAdminService.getTenantAdminDetail(id));
    }

    @Operation(summary = "禁用租户")
    @PostMapping("/{id}/disable")
    public Result<Void> disable(@PathVariable Long id) {
        tenantAdminService.disableTenant(id);
        return success();
    }

    @Operation(summary = "启用租户")
    @PostMapping("/{id}/enable")
    public Result<Void> enable(@PathVariable Long id) {
        tenantAdminService.enableTenant(id);
        return success();
    }

    @Operation(summary = "更新租户配置")
    @PutMapping("/{id}/config")
    public Result<Void> updateConfig(@PathVariable Long id, @RequestBody TenantConfigUpdateDTO dto) {
        tenantAdminService.updateTenantConfig(id, dto);
        return success();
    }
}
