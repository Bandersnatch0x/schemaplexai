package com.schemaplexai.quality.controller;

import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.quality.entity.SfQualityGate;
import com.schemaplexai.quality.service.QualityGateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/quality/gates")
@RequiredArgsConstructor
public class QualityGateController {

    private final QualityGateService qualityGateService;

    @PostMapping
    public Result<Long> create(@RequestBody SfQualityGate qualityGate) {
        qualityGateService.save(qualityGate);
        return Result.success(qualityGate.getId());
    }

    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody SfQualityGate qualityGate) {
        qualityGate.setId(id);
        return Result.success(qualityGateService.updateById(qualityGate));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(qualityGateService.removeById(id));
    }

    @GetMapping("/{id}")
    public Result<SfQualityGate> get(@PathVariable Long id) {
        SfQualityGate gate = qualityGateService.getById(id);
        if (gate == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        return Result.success(gate);
    }

    @GetMapping
    public Result<List<SfQualityGate>> list() {
        return Result.success(qualityGateService.list());
    }
}
