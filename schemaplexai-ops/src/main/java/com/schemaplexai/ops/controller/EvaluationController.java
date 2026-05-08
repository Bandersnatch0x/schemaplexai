package com.schemaplexai.ops.controller;

import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.ops.entity.SfEvalTask;
import com.schemaplexai.ops.service.EvaluationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ops/evaluations")
@RequiredArgsConstructor
@Tag(name = "评估任务管理", description = "评估任务执行、结果查询与数据集关联接口")
public class EvaluationController {

    private final EvaluationService evaluationService;

    @PostMapping
    @Operation(summary = "创建评估任务")
    public Result<Long> create(@RequestBody SfEvalTask evalTask) {
        evaluationService.save(evalTask);
        return Result.success(evalTask.getId());
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新评估任务")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody SfEvalTask evalTask) {
        evalTask.setId(id);
        return Result.success(evaluationService.updateById(evalTask));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除评估任务")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(evaluationService.removeById(id));
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取评估任务")
    public Result<SfEvalTask> get(@PathVariable Long id) {
        SfEvalTask evalTask = evaluationService.getById(id);
        if (evalTask == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        return Result.success(evalTask);
    }

    @GetMapping
    @Operation(summary = "列出所有评估任务")
    public Result<List<SfEvalTask>> list() {
        return Result.success(evaluationService.list());
    }

    @PostMapping("/{id}/run")
    @Operation(summary = "执行评估任务")
    public Result<SfEvalTask> runEvaluation(@PathVariable Long id) {
        return Result.success(evaluationService.runEvaluation(id));
    }

    @GetMapping("/{id}/results")
    @Operation(summary = "获取评估结果")
    public Result<SfEvalTask> getEvaluationResults(@PathVariable Long id) {
        return Result.success(evaluationService.getEvaluationResults(id));
    }

    @GetMapping("/by-dataset")
    @Operation(summary = "根据数据集列出评估任务")
    public Result<List<SfEvalTask>> listByDataset(@RequestParam Long datasetId) {
        return Result.success(evaluationService.listByDataset(datasetId));
    }

    @GetMapping("/by-status")
    @Operation(summary = "根据状态列出评估任务")
    public Result<List<SfEvalTask>> listByStatus(@RequestParam Integer status) {
        return Result.success(evaluationService.listByStatus(status));
    }
}
