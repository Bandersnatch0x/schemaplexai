package com.schemaplexai.spec.controller;

import com.schemaplexai.common.page.PageParam;
import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.model.dto.PageResult;
import com.schemaplexai.spec.entity.SfSpecSteering;
import com.schemaplexai.spec.service.SpecSteeringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/spec/steerings")
@RequiredArgsConstructor
@Tag(name = "规格导向规则管理")
public class SpecSteeringController {

    private final SpecSteeringService specSteeringService;

    @Operation(summary = "创建导向规则")
    @PostMapping
    public Result<Long> create(@RequestBody SfSpecSteering steering) {
        specSteeringService.save(steering);
        return Result.success(steering.getId());
    }

    @Operation(summary = "更新导向规则")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody SfSpecSteering steering) {
        steering.setId(id);
        return Result.success(specSteeringService.updateById(steering));
    }

    @Operation(summary = "删除导向规则")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(specSteeringService.removeById(id));
    }

    @Operation(summary = "获取导向规则详情")
    @GetMapping("/{id}")
    public Result<SfSpecSteering> get(@PathVariable Long id) {
        SfSpecSteering steering = specSteeringService.getById(id);
        if (steering == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        return Result.success(steering);
    }

    @Operation(summary = "分页查询导向规则")
    @GetMapping("/page")
    public Result<PageResult<SfSpecSteering>> page(PageParam pageParam) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<SfSpecSteering> page = specSteeringService.page(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageParam.getCurrent(), pageParam.getSize()));
        return Result.success(PageResult.of(page.getRecords(), page.getTotal(), pageParam.getCurrent(), pageParam.getSize()));
    }

    @Operation(summary = "评估导向规则")
    @PostMapping("/evaluate")
    public Result<Map<String, Boolean>> evaluateSteeringRules(@RequestParam Long specId,
                                                               @RequestParam String content) {
        return Result.success(specSteeringService.evaluateSteeringRules(specId, content));
    }

    @Operation(summary = "应用导向规则")
    @PostMapping("/apply")
    public Result<String> applySteering(@RequestParam Long specId,
                                         @RequestParam String content) {
        return Result.success(specSteeringService.applySteering(specId, content));
    }

    @Operation(summary = "获取生效的导向规则")
    @GetMapping("/active")
    public Result<List<SfSpecSteering>> listActiveSteerings(@RequestParam Long specId) {
        return Result.success(specSteeringService.listActiveSteerings(specId));
    }

    @Operation(summary = "验证导向规则配置")
    @PostMapping("/{id}/validate")
    public Result<Boolean> validateSteeringConfig(@PathVariable Long id) {
        return Result.success(specSteeringService.validateSteeringConfig(id));
    }
}
