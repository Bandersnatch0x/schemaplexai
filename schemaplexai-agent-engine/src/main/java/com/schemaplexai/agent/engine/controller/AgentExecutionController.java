package com.schemaplexai.agent.engine.controller;

import com.schemaplexai.agent.engine.AgentExecutionEngine;
import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import com.schemaplexai.agent.engine.lifecycle.AgentExecutionLifecycleService;
import com.schemaplexai.agent.engine.lifecycle.ExecutionSnapshot;
import com.schemaplexai.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/agents")
@RequiredArgsConstructor
public class AgentExecutionController {

    private final AgentExecutionEngine executionEngine;
    private final AgentExecutionLifecycleService lifecycleService;

    @PostMapping("/{id}/execute")
    public Result<SfAgentExecution> execute(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String tenantId = body.get("tenantId");
        String prompt = body.get("prompt");
        SfAgentExecution execution = executionEngine.startExecution(id, tenantId, prompt);
        return Result.success(execution);
    }

    @PostMapping("/{id}/executions/{execId}/pause")
    public Result<Void> pause(@PathVariable Long id, @PathVariable Long execId, @RequestParam String reason) {
        lifecycleService.pauseExecution(execId, com.schemaplexai.agent.engine.lifecycle.PauseReason.valueOf(reason));
        return Result.success();
    }

    @PostMapping("/{id}/executions/{execId}/resume")
    public Result<Void> resume(@PathVariable Long id, @PathVariable Long execId) {
        lifecycleService.resumeExecution(execId);
        return Result.success();
    }

    @PostMapping("/{id}/executions/{execId}/cancel")
    public Result<Void> cancel(@PathVariable Long id, @PathVariable Long execId) {
        lifecycleService.cancelExecution(execId);
        return Result.success();
    }

    @GetMapping("/{id}/executions/{execId}/snapshot")
    public Result<ExecutionSnapshot> snapshot(@PathVariable Long id, @PathVariable Long execId) {
        ExecutionSnapshot snapshot = lifecycleService.getLatestSnapshot(execId);
        return Result.success(snapshot);
    }
}
