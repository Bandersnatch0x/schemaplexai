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
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional(rollbackFor = Exception.class)
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
            nodeExecution.setOutputJson("{\"error\":\"" + e.getMessage() + "\"}");
            nodeExecutionMapper.updateById(nodeExecution);
            throw new BaseException(ResultCode.ERROR, "Node execution failed: " + e.getMessage());
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
