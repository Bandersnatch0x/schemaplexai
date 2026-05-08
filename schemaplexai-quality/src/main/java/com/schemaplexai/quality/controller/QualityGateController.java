package com.schemaplexai.quality.controller;

import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.quality.entity.SfQualityGate;
import com.schemaplexai.quality.service.QualityGateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/quality/gates")
@RequiredArgsConstructor
@Tag(name = "质量门禁管理")
public class QualityGateController {

    private final QualityGateService qualityGateService;

    @Operation(summary = "创建质量门禁")
    @PostMapping
    public Result<Long> create(@RequestBody SfQualityGate qualityGate) {
        qualityGateService.save(qualityGate);
        return Result.success(qualityGate.getId());
    }

    @Operation(summary = "更新质量门禁")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody SfQualityGate qualityGate) {
        qualityGate.setId(id);
        return Result.success(qualityGateService.updateById(qualityGate));
    }

    @Operation(summary = "删除质量门禁")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(qualityGateService.removeById(id));
    }

    @Operation(summary = "获取质量门禁详情")
    @GetMapping("/{id}")
    public Result<SfQualityGate> get(@PathVariable Long id) {
        SfQualityGate gate = qualityGateService.getById(id);
        if (gate == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        return Result.success(gate);
    }

    @Operation(summary = "获取质量门禁列表")
    @GetMapping
    public Result<List<SfQualityGate>> list() {
        return Result.success(qualityGateService.list());
    }
}
