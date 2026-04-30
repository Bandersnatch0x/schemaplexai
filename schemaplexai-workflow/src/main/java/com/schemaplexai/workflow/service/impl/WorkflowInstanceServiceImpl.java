package com.schemaplexai.workflow.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.workflow.entity.SfWorkflowInstance;
import com.schemaplexai.workflow.entity.SfWorkflowNodeExecution;
import com.schemaplexai.workflow.entity.SfWorkflowTemplate;
import com.schemaplexai.workflow.mapper.SfWorkflowInstanceMapper;
import com.schemaplexai.workflow.mapper.SfWorkflowNodeExecutionMapper;
import com.schemaplexai.workflow.mapper.SfWorkflowTemplateMapper;
import com.schemaplexai.workflow.node.NodeExecutionResult;
import com.schemaplexai.workflow.service.WorkflowInstanceService;
import com.schemaplexai.workflow.service.WorkflowNodeEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowInstanceServiceImpl extends ServiceImpl<SfWorkflowInstanceMapper, SfWorkflowInstance> implements WorkflowInstanceService {

    private final SfWorkflowTemplateMapper templateMapper;
    private final SfWorkflowNodeExecutionMapper nodeExecutionMapper;
    private final WorkflowNodeEngine nodeEngine;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void trigger(Long instanceId) {
        SfWorkflowInstance instance = baseMapper.selectById(instanceId);
        if (instance == null) {
            throw new BaseException(ResultCode.WORKFLOW_NOT_FOUND, "Workflow instance not found: " + instanceId);
        }

        SfWorkflowTemplate template = templateMapper.selectById(instance.getTemplateId());
        if (template == null) {
            throw new BaseException(ResultCode.WORKFLOW_NOT_FOUND, "Workflow template not found: " + instance.getTemplateId());
        }

        instance.setStatus("RUNNING");
        baseMapper.updateById(instance);

        List<NodeConfig> nodeConfigs = parseNodeConfig(template.getNodeConfigJson());
        for (NodeConfig config : nodeConfigs) {
            SfWorkflowNodeExecution nodeExecution = new SfWorkflowNodeExecution();
            nodeExecution.setInstanceId(instanceId);
            nodeExecution.setNodeId(config.getNodeId());
            nodeExecution.setNodeType(config.getNodeType());
            nodeExecution.setStatus("PENDING");
            try {
                nodeExecution.setInputJson(objectMapper.writeValueAsString(config.getInput()));
            } catch (Exception e) {
                nodeExecution.setInputJson("{}");
            }
            nodeExecutionMapper.insert(nodeExecution);

            NodeExecutionResult result = nodeEngine.executeNode(nodeExecution);
            if (!result.isSuccess()) {
                instance.setStatus("FAILED");
                baseMapper.updateById(instance);
                log.warn("Workflow instance {} failed at node {}", instanceId, config.getNodeId());
                return;
            }
        }

        instance.setStatus("COMPLETED");
        baseMapper.updateById(instance);
        log.info("Workflow instance {} completed successfully", instanceId);
    }

    private List<NodeConfig> parseNodeConfig(String nodeConfigJson) {
        if (nodeConfigJson == null || nodeConfigJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(nodeConfigJson, new TypeReference<List<NodeConfig>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse node config JSON, using empty list", e);
            return List.of();
        }
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class NodeConfig {
        private String nodeId;
        private String nodeType;
        private Map<String, Object> input;
    }
}
