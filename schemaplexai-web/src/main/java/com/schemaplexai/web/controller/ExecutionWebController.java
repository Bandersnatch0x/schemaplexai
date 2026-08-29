package com.schemaplexai.web.controller;

import com.schemaplexai.common.controller.BaseController;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.schemaplexai.common.result.Result;
import com.schemaplexai.web.service.execution.EngineExecutionQueryPort;
import com.schemaplexai.web.service.execution.ExecutionLifecyclePort;
import com.schemaplexai.web.service.execution.ExecutionStatusPort;
import com.schemaplexai.web.vo.ExecutionStatusVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * M6.4: Execution Web Controller.
 * Provides HTTP API for execution lifecycle control: status, pause, resume, cancel.
 */
@RestController
@RequestMapping("/web/executions")
@Tag(name = "执行控制", description = "执行生命周期控制 API")
@RequiredArgsConstructor
@Validated
public class ExecutionWebController extends BaseController {

    private final ExecutionLifecyclePort lifecyclePort;
    private final ExecutionStatusPort statusPort;
    private final EngineExecutionQueryPort queryPort;

    @Operation(summary = "获取执行状态")
    @GetMapping("/{id}")
    public Result<ExecutionStatusVO> getExecutionStatus(@PathVariable Long id) {
        return success(statusPort.getExecutionStatus(id));
    }

    @Operation(summary = "分页查询执行列表")
    @GetMapping
    public Result<IPage<ExecutionStatusVO>> listExecutions(
            @Parameter(description = "页码，默认1") @Min(1) @RequestParam(defaultValue = "1") long page,
            @Parameter(description = "每页大小，默认20") @Min(1) @Max(100) @RequestParam(defaultValue = "20") long size,
            @Parameter(description = "按状态筛选") @RequestParam(required = false) String state,
            @Parameter(description = "按Agent ID筛选") @RequestParam(required = false) Long agentId) {
        return success(queryPort.listExecutions(page, size, state, agentId));
    }

    @Operation(summary = "暂停执行")
    @PostMapping("/{id}/pause")
    public Result<Void> pauseExecution(@PathVariable Long id) {
        lifecyclePort.pauseExecution(id);
        return success();
    }

    @Operation(summary = "恢复执行")
    @PostMapping("/{id}/resume")
    public Result<Void> resumeExecution(@PathVariable Long id) {
        lifecyclePort.resumeExecution(id);
        return success();
    }

    @Operation(summary = "取消执行")
    @PostMapping("/{id}/cancel")
    public Result<Void> cancelExecution(@PathVariable Long id) {
        lifecyclePort.cancelExecution(id);
        return success();
    }
}
