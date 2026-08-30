package com.schemaplexai.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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

import java.time.LocalDateTime;
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
            markFailed(instance);
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
                markFailed(instance);
                throw new BaseException(ResultCode.WORKFLOW_NOT_FOUND, e.getMessage());
            }
        } else {
            // First trigger: store the topology hash
            instance.setTopologyHash(currentHash);
        }

        instance.setStatus("RUNNING");
        if (instance.getStartedAt() == null) {
            instance.setStartedAt(LocalDateTime.now());
        }
        baseMapper.updateById(instance);

        runNodes(instance, template);
    }

    @Override
    public void cancel(Long instanceId) {
        // Spec §6.2 / §4: user cancellation drives the CANCELLED terminal state. Execution
        // is synchronous per request thread, so a concurrently running instance observes
        // the cancellation at the next node boundary (runNodes re-reads the status) and
        // stops there without starting further nodes.
        SfWorkflowInstance instance = baseMapper.selectById(instanceId);
        if (instance == null) {
            throw new BaseException(ResultCode.WORKFLOW_NOT_FOUND, "Workflow instance not found: " + instanceId);
        }
        String status = instance.getStatus();
        if (!"PENDING".equals(status) && !"RUNNING".equals(status) && !"WAITING_APPROVAL".equals(status)) {
            throw new BaseException(ResultCode.CONFLICT,
                    "Workflow instance " + instanceId + " cannot be cancelled in status " + status);
        }
        instance.setStatus("CANCELLED");
        instance.setCompletedAt(LocalDateTime.now());
        baseMapper.updateById(instance);
        log.info("Workflow instance {} cancelled", instanceId);
    }

    @Override
    public void approve(Long instanceId, String comment) {
        // Spec §3.5 step 3 / §6.2: approval callback resumes the paused workflow.
        SfWorkflowInstance instance = requireWaitingApproval(instanceId, "approved");
        SfWorkflowTemplate template = templateMapper.selectById(instance.getTemplateId());
        if (template == null) {
            throw new BaseException(ResultCode.WORKFLOW_NOT_FOUND,
                    "Workflow template not found: " + instance.getTemplateId());
        }

        SfWorkflowNodeExecution approvalNode = findWaitingApprovalNode(instanceId);
        approvalNode.setStatus("COMPLETED");
        approvalNode.setOutputJson(writeJson(Map.of(
                "approved", true,
                "comment", comment != null ? comment : "",
                "decidedAt", LocalDateTime.now().toString())));
        nodeExecutionMapper.updateById(approvalNode);

        instance.setStatus("RUNNING");
        baseMapper.updateById(instance);
        log.info("Workflow instance {} approved, resuming execution", instanceId);
        // runNodes skips already-completed node executions, so execution continues after
        // the approved gate instead of re-running upstream nodes.
        runNodes(instance, template);
    }

    @Override
    public void reject(Long instanceId, String reason) {
        // Spec §6.2: rejection terminates the paused workflow as FAILED (spec §4 has no
        // distinct rejected terminal state).
        SfWorkflowInstance instance = requireWaitingApproval(instanceId, "rejected");

        SfWorkflowNodeExecution approvalNode = findWaitingApprovalNode(instanceId);
        approvalNode.setStatus("FAILED");
        approvalNode.setOutputJson(writeJson(Map.of(
                "approved", false,
                "reason", reason != null ? reason : "",
                "decidedAt", LocalDateTime.now().toString())));
        nodeExecutionMapper.updateById(approvalNode);

        markFailed(instance);
        log.info("Workflow instance {} rejected", instanceId);
    }

    /**
     * Runs the flat node list of the template for one execution leg. Node executions
     * already COMPLETED (an earlier leg before an approval pause) are skipped and their
     * persisted outputs re-seed the substitution context, so resume after approve()
     * continues where the workflow paused instead of re-executing upstream nodes.
     */
    private void runNodes(SfWorkflowInstance instance, SfWorkflowTemplate template) {
        List<NodeConfig> nodeConfigs = parseNodeConfig(template.getNodeConfigJson());

        // Inter-node data flow (spec §3.2): upstream node outputs become available to
        // downstream nodes. "input" is the instance-level input merged with the output of
        // all upstream nodes; each upstream node is also addressable individually by its
        // nodeId.
        Map<String, Object> mergedUpstreamOutput = parseJsonMap(instance.getInputData());
        Map<String, Object> nodeOutputs = new HashMap<>();
        Map<String, Object> workflowOutput = new HashMap<>();
        Map<String, SfWorkflowNodeExecution> completedNodes = new HashMap<>();
        for (SfWorkflowNodeExecution executed : listNodeExecutions(instance.getId())) {
            if (!"COMPLETED".equals(executed.getStatus())) {
                continue;
            }
            completedNodes.put(executed.getNodeId(), executed);
            Map<String, Object> output = parseJsonMap(executed.getOutputJson());
            if (output.isEmpty()) {
                continue;
            }
            mergedUpstreamOutput.putAll(output);
            nodeOutputs.put(executed.getNodeId(), output);
            workflowOutput.putAll(output);
        }

        for (NodeConfig config : nodeConfigs) {
            // Cancellation is honored at node boundaries: cancel() flips the instance
            // status and the loop stops before the next node starts.
            if (isCancelled(instance.getId())) {
                log.info("Workflow instance {} cancelled, stopping execution", instance.getId());
                return;
            }

            if (completedNodes.containsKey(config.getNodeId())) {
                // Resuming after an approval pause: upstream work already ran and is recorded.
                continue;
            }

            Map<String, Object> substitutionContext = new HashMap<>(nodeOutputs);
            substitutionContext.put("input", mergedUpstreamOutput);
            Map<String, Object> resolvedInput =
                    NodeVariableSubstitutor.substitute(config.getInput(), substitutionContext);

            if ("HUMAN_APPROVAL".equals(config.getNodeType())) {
                // Spec §3.5: pause the instance until approve()/reject() decides the gate.
                SfWorkflowNodeExecution approvalNode = new SfWorkflowNodeExecution();
                approvalNode.setInstanceId(instance.getId());
                approvalNode.setNodeId(config.getNodeId());
                approvalNode.setNodeType(config.getNodeType());
                approvalNode.setStatus("WAITING_APPROVAL");
                approvalNode.setInputJson(writeJson(resolvedInput));
                approvalNode.setTenantId(instance.getTenantId());
                nodeExecutionMapper.insert(approvalNode);

                instance.setStatus("WAITING_APPROVAL");
                baseMapper.updateById(instance);
                log.info("Workflow instance {} waiting for human approval at node {}",
                        instance.getId(), config.getNodeId());
                return;
            }

            SfWorkflowNodeExecution nodeExecution = new SfWorkflowNodeExecution();
            nodeExecution.setInstanceId(instance.getId());
            nodeExecution.setNodeId(config.getNodeId());
            nodeExecution.setNodeType(config.getNodeType());
            nodeExecution.setStatus("PENDING");
            nodeExecution.setInputJson(writeJson(resolvedInput));
            nodeExecution.setTenantId(instance.getTenantId());
            nodeExecutionMapper.insert(nodeExecution);

            NodeExecutionResult result;
            try {
                result = nodeEngine.executeNode(nodeExecution);
            } catch (RuntimeException e) {
                // Exception path: persist FAILED instance state before propagating, so the
                // failure is observable in the database instead of a permanently RUNNING row.
                markFailed(instance);
                log.warn("Workflow instance {} failed with exception at node {}: {}",
                        instance.getId(), config.getNodeId(), e.getMessage());
                throw e;
            }
            if (!result.isSuccess()) {
                markFailed(instance);
                log.warn("Workflow instance {} failed at node {}", instance.getId(), config.getNodeId());
                return;
            }

            // Propagate this node's output to downstream nodes.
            if (result.getOutput() != null) {
                mergedUpstreamOutput.putAll(result.getOutput());
                nodeOutputs.put(config.getNodeId(), result.getOutput());
                workflowOutput.putAll(result.getOutput());
            }
        }

        instance.setStatus("COMPLETED");
        instance.setOutputData(writeJson(workflowOutput));
        instance.setCompletedAt(LocalDateTime.now());
        baseMapper.updateById(instance);
        log.info("Workflow instance {} completed successfully", instance.getId());
    }

    private SfWorkflowInstance requireWaitingApproval(Long instanceId, String action) {
        SfWorkflowInstance instance = baseMapper.selectById(instanceId);
        if (instance == null) {
            throw new BaseException(ResultCode.WORKFLOW_NOT_FOUND, "Workflow instance not found: " + instanceId);
        }
        if (!"WAITING_APPROVAL".equals(instance.getStatus())) {
            throw new BaseException(ResultCode.CONFLICT,
                    "Workflow instance " + instanceId + " cannot be " + action + " in status "
                            + instance.getStatus() + " (expected WAITING_APPROVAL)");
        }
        return instance;
    }

    private SfWorkflowNodeExecution findWaitingApprovalNode(Long instanceId) {
        List<SfWorkflowNodeExecution> waiting = nodeExecutionMapper.selectList(
                new LambdaQueryWrapper<SfWorkflowNodeExecution>()
                        .eq(SfWorkflowNodeExecution::getInstanceId, instanceId)
                        .eq(SfWorkflowNodeExecution::getStatus, "WAITING_APPROVAL")
                        .orderByDesc(SfWorkflowNodeExecution::getId));
        if (waiting.isEmpty()) {
            throw new BaseException(ResultCode.CONFLICT,
                    "Workflow instance " + instanceId + " has no pending HUMAN_APPROVAL node execution");
        }
        return waiting.get(0);
    }

    private boolean isCancelled(Long instanceId) {
        SfWorkflowInstance current = baseMapper.selectById(instanceId);
        return current != null && "CANCELLED".equals(current.getStatus());
    }

    private void markFailed(SfWorkflowInstance instance) {
        instance.setStatus("FAILED");
        instance.setCompletedAt(LocalDateTime.now());
        baseMapper.updateById(instance);
    }

    private List<SfWorkflowNodeExecution> listNodeExecutions(Long instanceId) {
        return nodeExecutionMapper.selectList(new LambdaQueryWrapper<SfWorkflowNodeExecution>()
                .eq(SfWorkflowNodeExecution::getInstanceId, instanceId)
                .orderByAsc(SfWorkflowNodeExecution::getId));
    }

    private Map<String, Object> parseJsonMap(String json) {
        if (json == null || json.isBlank()) {
            return new HashMap<>();
        }
        try {
            Map<String, Object> parsed = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
            return parsed != null ? new HashMap<>(parsed) : new HashMap<>();
        } catch (Exception e) {
            log.warn("Failed to parse JSON map, using empty map", e);
            return new HashMap<>();
        }
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
