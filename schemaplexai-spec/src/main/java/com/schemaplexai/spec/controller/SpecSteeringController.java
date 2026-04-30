package com.schemaplexai.spec.controller;

import com.schemaplexai.common.page.PageParam;
import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.model.dto.PageResult;
import com.schemaplexai.spec.entity.SfSpecSteering;
import com.schemaplexai.spec.service.SpecSteeringService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/spec/steerings")
@RequiredArgsConstructor
public class SpecSteeringController {

    private final SpecSteeringService specSteeringService;

    @PostMapping
    public Result<Long> create(@RequestBody SfSpecSteering steering) {
        specSteeringService.save(steering);
        return Result.success(steering.getId());
    }

    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody SfSpecSteering steering) {
        steering.setId(id);
        return Result.success(specSteeringService.updateById(steering));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(specSteeringService.removeById(id));
    }

    @GetMapping("/{id}")
    public Result<SfSpecSteering> get(@PathVariable Long id) {
        SfSpecSteering steering = specSteeringService.getById(id);
        if (steering == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        return Result.success(steering);
    }

    @GetMapping("/page")
    public Result<PageResult<SfSpecSteering>> page(PageParam pageParam) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<SfSpecSteering> page = specSteeringService.page(
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageParam.getCurrent(), pageParam.getSize()));
        return Result.success(PageResult.of(page.getRecords(), page.getTotal(), pageParam.getCurrent(), pageParam.getSize()));
    }
}
