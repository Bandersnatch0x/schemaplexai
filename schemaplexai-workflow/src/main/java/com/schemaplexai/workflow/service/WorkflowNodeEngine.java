package com.schemaplexai.workflow.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.workflow.entity.SfWorkflowNodeExecution;
import com.schemaplexai.workflow.mapper.SfWorkflowNodeExecutionMapper;
import com.schemaplexai.workflow.node.NodeExecutionResult;
import com.schemaplexai.workflow.node.NodeExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowNodeEngine {

    private final List<NodeExecutor> executorList;
    private final SfWorkflowNodeExecutionMapper nodeExecutionMapper;
    private final ObjectMapper objectMapper;

    private Map<String, NodeExecutor> executors;

    @jakarta.annotation.PostConstruct
    public void init() {
        this.executors = executorList.stream()
                .collect(Collectors.toMap(NodeExecutor::getNodeType, Function.identity()));
    }

    // Deliberately NOT @Transactional: node execution wraps external calls (LLM/HTTP) and
    // must record FAILED status durably even when the execution throws. Inside a wrapping
    // transaction the failure-status update was rolled back along with the rethrown
    // exception, leaving no trace of the failure in the database.
    public NodeExecutionResult executeNode(SfWorkflowNodeExecution nodeExecution) {
        NodeExecutor executor = executors.get(nodeExecution.getNodeType());
        if (executor == null) {
            throw new BaseException(ResultCode.ERROR,
                    "No executor for node type: " + nodeExecution.getNodeType());
        }

        nodeExecution.setStatus("RUNNING");
        nodeExecutionMapper.updateById(nodeExecution);

        try {
            Map<String, Object> input = parseInput(nodeExecution.getInputJson());
            NodeExecutionResult result = executor.execute(input, nodeExecution.getTenantId());

            nodeExecution.setStatus(result.isSuccess() ? "COMPLETED" : "FAILED");
            nodeExecution.setOutputJson(objectMapper.writeValueAsString(result.getOutput()));
            nodeExecutionMapper.updateById(nodeExecution);

            return result;
        } catch (Exception e) {
            log.error("Node execution failed: nodeId={}, instanceId={}",
                    nodeExecution.getNodeId(), nodeExecution.getInstanceId(), e);
            nodeExecution.setStatus("FAILED");
            nodeExecution.setOutputJson(toErrorJson(e.getMessage()));
            nodeExecutionMapper.updateById(nodeExecution);
            throw new BaseException(ResultCode.ERROR, "Node execution failed: " + e.getMessage());
        }
    }

    private String toErrorJson(String message) {
        try {
            return objectMapper.writeValueAsString(Map.of("error", message != null ? message : "unknown"));
        } catch (Exception e) {
            return "{\"error\":\"unknown\"}";
        }
    }

    private Map<String, Object> parseInput(String inputJson) {
        if (inputJson == null || inputJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(inputJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse input JSON, using empty map", e);
            return Map.of();
        }
    }
}
