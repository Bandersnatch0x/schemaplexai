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
import com.schemaplexai.workflow.service.NodeVariableSubstitutor;
import com.schemaplexai.workflow.service.TopologyHasher;
import com.schemaplexai.workflow.service.TopologyMismatchException;
import com.schemaplexai.workflow.service.WorkflowInstanceService;
import com.schemaplexai.workflow.service.WorkflowNodeEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
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
    public void trigger(Long instanceId) {
        // Deliberately NOT @Transactional: node execution performs long external calls
        // (LLM/HTTP) and every status transition (RUNNING/FAILED/COMPLETED) must be
        // durable the moment it is written. A wrapping transaction would roll back the
        // FAILED status on exception paths, leaving no failure trace in the database.
        SfWorkflowInstance instance = baseMapper.selectById(instanceId);
        if (instance == null) {
            throw new BaseException(ResultCode.WORKFLOW_NOT_FOUND, "Workflow instance not found: " + instanceId);
        }

        SfWorkflowTemplate template = templateMapper.selectById(instance.getTemplateId());
        if (template == null) {
            throw new BaseException(ResultCode.WORKFLOW_NOT_FOUND, "Workflow template not found: " + instance.getTemplateId());
        }

        // Spec §4: only published templates may be executed; DRAFT/inactive templates are rejected.
        if (!WorkflowTemplateServiceImpl.STATUS_DEPLOYED.equals(template.getStatus())) {
            log.warn("Refusing to trigger instance {}: template {} is not published (status={})",
                    instanceId, instance.getTemplateId(), template.getStatus());
            instance.setStatus("FAILED");
            baseMapper.updateById(instance);
            throw new BaseException(ResultCode.PARAM_ERROR,
                    "Workflow template is not published (status=" + template.getStatus()
                            + "); deploy it before triggering instances: " + instance.getTemplateId());
        }

        // Validate topology hash on resume (prevents silent corruption)
        String currentHash = TopologyHasher.hash(template.getNodeConfigJson());
        if (instance.getTopologyHash() != null) {
            try {
                TopologyHasher.verify(instance.getTopologyHash(), template.getNodeConfigJson());
            } catch (TopologyMismatchException e) {
                log.error("Topology mismatch for workflow instance {}: {}", instanceId, e.getMessage());
                instance.setStatus("FAILED");
                baseMapper.updateById(instance);
                throw new BaseException(ResultCode.WORKFLOW_NOT_FOUND, e.getMessage());
            }
        } else {
            // First trigger: store the topology hash
            instance.setTopologyHash(currentHash);
        }

        instance.setStatus("RUNNING");
        baseMapper.updateById(instance);

        List<NodeConfig> nodeConfigs = parseNodeConfig(template.getNodeConfigJson());

        // Inter-node data flow (spec §3.2): upstream node outputs become available to
        // downstream nodes. "input" is the merged output of all upstream nodes; each
        // upstream node is also addressable individually by its nodeId.
        Map<String, Object> mergedUpstreamOutput = new HashMap<>();
        Map<String, Object> nodeOutputs = new HashMap<>();

        for (NodeConfig config : nodeConfigs) {
            Map<String, Object> substitutionContext = new HashMap<>(nodeOutputs);
            substitutionContext.put("input", mergedUpstreamOutput);
            Map<String, Object> resolvedInput =
                    NodeVariableSubstitutor.substitute(config.getInput(), substitutionContext);

            SfWorkflowNodeExecution nodeExecution = new SfWorkflowNodeExecution();
            nodeExecution.setInstanceId(instanceId);
            nodeExecution.setNodeId(config.getNodeId());
            nodeExecution.setNodeType(config.getNodeType());
            nodeExecution.setStatus("PENDING");
            nodeExecution.setInputJson(writeJson(resolvedInput));
            nodeExecutionMapper.insert(nodeExecution);

            NodeExecutionResult result;
            try {
                result = nodeEngine.executeNode(nodeExecution);
            } catch (RuntimeException e) {
                // Exception path: persist FAILED instance state before propagating, so the
                // failure is observable in the database instead of a permanently RUNNING row.
                instance.setStatus("FAILED");
                baseMapper.updateById(instance);
                log.warn("Workflow instance {} failed with exception at node {}: {}",
                        instanceId, config.getNodeId(), e.getMessage());
                throw e;
            }
            if (!result.isSuccess()) {
                instance.setStatus("FAILED");
                baseMapper.updateById(instance);
                log.warn("Workflow instance {} failed at node {}", instanceId, config.getNodeId());
                return;
            }

            // Propagate this node's output to downstream nodes.
            if (result.getOutput() != null) {
                mergedUpstreamOutput.putAll(result.getOutput());
                nodeOutputs.put(config.getNodeId(), result.getOutput());
            }
        }

        instance.setStatus("COMPLETED");
        baseMapper.updateById(instance);
        log.info("Workflow instance {} completed successfully", instanceId);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "{}";
        }
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
