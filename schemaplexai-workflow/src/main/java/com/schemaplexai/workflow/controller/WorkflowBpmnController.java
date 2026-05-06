package com.schemaplexai.workflow.controller;

import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.workflow.service.WorkflowDeployService;
import com.schemaplexai.workflow.service.WorkflowDeployService.ProcessDefinitionInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for Flowable BPMN workflow operations.
 * Provides endpoints for listing deployed processes and starting process instances.
 */
@Slf4j
@RestController
@RequestMapping("/workflow/bpmn")
@RequiredArgsConstructor
public class WorkflowBpmnController {

    private final WorkflowDeployService workflowDeployService;

    /**
     * List all deployed (active) BPMN process definitions.
     */
    @GetMapping("/processes")
    public Result<List<ProcessDefinitionInfo>> listDeployedProcesses() {
        return Result.success(workflowDeployService.listDeployedProcesses());
    }

    /**
     * Start a new process instance by process definition key.
     *
     * @param processKey the BPMN process key (e.g., "specReviewApproval")
     * @param request    contains businessKey and process variables
     * @return the started process instance ID
     */
    @PostMapping("/processes/{processKey}/start")
    public Result<String> startProcessInstance(
            @PathVariable String processKey,
            @RequestBody StartProcessRequest request) {
        try {
            String instanceId = workflowDeployService.startProcessInstance(
                    processKey, request.businessKey(), request.variables());
            return Result.success(instanceId);
        } catch (Exception e) {
            log.error("[WorkflowBpmn] Failed to start process key={}", processKey, e);
            return Result.error(ResultCode.WORKFLOW_NOT_FOUND.getCode(),
                    "Failed to start process: " + e.getMessage());
        }
    }

    /**
     * Suspend a process definition by key.
     */
    @PostMapping("/processes/{processKey}/suspend")
    public Result<Boolean> suspendProcessDefinition(@PathVariable String processKey) {
        workflowDeployService.suspendProcessDefinition(processKey);
        return Result.success(true);
    }

    /**
     * Activate a suspended process definition by key.
     */
    @PostMapping("/processes/{processKey}/activate")
    public Result<Boolean> activateProcessDefinition(@PathVariable String processKey) {
        workflowDeployService.activateProcessDefinition(processKey);
        return Result.success(true);
    }

    /**
     * Request body for starting a process instance.
     */
    public record StartProcessRequest(String businessKey, Map<String, Object> variables) {
    }
}
