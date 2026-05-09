package com.schemaplexai.web.controller;

import com.schemaplexai.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * M6.4: Execution Web Controller.
 * Provides HTTP API for execution lifecycle control: status, pause, resume, cancel.
 */
@RestController
@RequestMapping("/web/executions")
@Tag(name = "执行控制", description = "执行生命周期控制 API")
public class ExecutionWebController extends BaseController {

    @Operation(summary = "获取执行状态")
    @GetMapping("/{id}")
    public Result<Map<String, Object>> getExecutionStatus(@PathVariable Long id) {
        Map<String, Object> status = new HashMap<>();
        status.put("executionId", id);
        status.put("status", "RUNNING");
        status.put("progress", 65);
        status.put("startedAt", System.currentTimeMillis() - 300000);
        return success(status);
    }

    @Operation(summary = "暂停执行")
    @PostMapping("/{id}/pause")
    public Result<Void> pauseExecution(@PathVariable Long id) {
        // TODO: delegate to execution service
        return success();
    }

    @Operation(summary = "恢复执行")
    @PostMapping("/{id}/resume")
    public Result<Void> resumeExecution(@PathVariable Long id) {
        // TODO: delegate to execution service
        return success();
    }

    @Operation(summary = "取消执行")
    @PostMapping("/{id}/cancel")
    public Result<Void> cancelExecution(@PathVariable Long id) {
        // TODO: delegate to execution service
        return success();
    }
}
