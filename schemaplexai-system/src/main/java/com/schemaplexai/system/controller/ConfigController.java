package com.schemaplexai.system.controller;

import com.schemaplexai.common.page.PageParam;
import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.model.dto.PageResult;
import com.schemaplexai.system.entity.SfConfig;
import com.schemaplexai.system.service.ConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "系统配置管理")
@RestController
@RequestMapping("/system/configs")
@RequiredArgsConstructor
public class ConfigController {

    private final ConfigService configService;

    @Operation(summary = "分页查询系统配置")
    @GetMapping
    public Result<PageResult<SfConfig>> page(PageParam pageParam) {
        var page = configService.page(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageParam.getCurrent(), pageParam.getSize()));
        return Result.success(PageResult.of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize()));
    }

    @Operation(summary = "获取系统配置详情")
    @GetMapping("/{id}")
    public Result<SfConfig> getById(@PathVariable Long id) {
        SfConfig config = configService.getById(id);
        if (config == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        return Result.success(config);
    }

    @Operation(summary = "创建系统配置")
    @PostMapping
    public Result<Long> create(@RequestBody SfConfig config) {
        configService.save(config);
        return Result.success(config.getId());
    }

    @Operation(summary = "更新系统配置")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody SfConfig config) {
        config.setId(id);
        return Result.success(configService.updateById(config));
    }

    @Operation(summary = "删除系统配置")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(configService.removeById(id));
    }
}
