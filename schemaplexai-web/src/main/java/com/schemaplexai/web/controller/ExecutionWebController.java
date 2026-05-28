package com.schemaplexai.web.controller;

import com.schemaplexai.common.result.Result;
import com.schemaplexai.web.service.execution.ExecutionLifecyclePort;
import com.schemaplexai.web.service.execution.ExecutionStatusPort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * M6.4: Execution Web Controller.
 * Provides HTTP API for execution lifecycle control: status, pause, resume, cancel.
 */
@RestController
@RequestMapping("/web/executions")
@Tag(name = "执行控制", description = "执行生命周期控制 API")
@RequiredArgsConstructor
public class ExecutionWebController extends BaseController {

    private final ExecutionLifecyclePort lifecyclePort;
    private final ExecutionStatusPort statusPort;

    @Operation(summary = "获取执行状态")
    @GetMapping("/{id}")
    public Result<Map<String, Object>> getExecutionStatus(@PathVariable Long id) {
        return success(statusPort.getExecutionStatus(id));
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
