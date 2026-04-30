package com.schemaplexai.ops.controller;

import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.ops.entity.SfEvalTask;
import com.schemaplexai.ops.service.EvaluationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ops/evaluations")
@RequiredArgsConstructor
public class EvaluationController {

    private final EvaluationService evaluationService;

    @PostMapping
    public Result<Long> create(@RequestBody SfEvalTask evalTask) {
        evaluationService.save(evalTask);
        return Result.success(evalTask.getId());
    }

    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody SfEvalTask evalTask) {
        evalTask.setId(id);
        return Result.success(evaluationService.updateById(evalTask));
    }

    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.success(evaluationService.removeById(id));
    }

    @GetMapping("/{id}")
    public Result<SfEvalTask> get(@PathVariable Long id) {
        SfEvalTask evalTask = evaluationService.getById(id);
        if (evalTask == null) {
            return Result.error(ResultCode.NOT_FOUND);
        }
        return Result.success(evalTask);
    }

    @GetMapping
    public Result<List<SfEvalTask>> list() {
        return Result.success(evaluationService.list());
    }
}
