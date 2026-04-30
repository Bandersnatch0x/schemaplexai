package com.schemaplexai.system.controller;

import com.schemaplexai.common.page.PageParam;
import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.model.dto.PageResult;
import com.schemaplexai.system.entity.SfTenant;
import com.schemaplexai.system.service.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "租户管理")
@RestController
@RequestMapping("/system/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @Operation(summary = "分页查询租户")
    @GetMapping
    public Result<PageResult<SfTenant>> page(PageParam pageParam) {
        var page = tenantService.page(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageParam.getCurrent(), pageParam.getSize()));
        return Result.success(PageResult.of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize()));
    }

    @Operation(summary = "获取租户详情")
    @GetMapping("/{id}")
    public Result<SfTenant> getById(@PathVariable Long id) {
        SfTenant tenant = tenantService.getById(id);
        if (tenant == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        return Result.success(tenant);
    }

    @Operation(summary = "创建租户")
    @PostMapping
    public Result<Long> create(@RequestBody SfTenant tenant) {
        tenantService.save(tenant);
        return Result.success(tenant.getId());
    }

    @Operation(summary = "更新租户")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody SfTenant tenant) {
        tenant.setId(id);
        return Result.success(tenantService.updateById(tenant));
    }

    @Operation(summary = "删除租户")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(tenantService.removeById(id));
    }
}
