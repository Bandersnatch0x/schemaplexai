package com.schemaplexai.workflow.service;

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
import com.schemaplexai.workflow.service.TopologyHasher;
import com.schemaplexai.workflow.service.impl.WorkflowInstanceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkflowInstanceServiceImplTest {

    @Mock
    private SfWorkflowInstanceMapper workflowInstanceMapper;

    @Mock
    private SfWorkflowTemplateMapper templateMapper;

    @Mock
    private SfWorkflowNodeExecutionMapper nodeExecutionMapper;

    @Mock
    private WorkflowNodeEngine nodeEngine;

    @InjectMocks
    private WorkflowInstanceServiceImpl workflowInstanceService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(workflowInstanceService, "baseMapper", workflowInstanceMapper);
        ReflectionTestUtils.setField(workflowInstanceService, "objectMapper", new ObjectMapper());
    }

    // ------------------------------------------------------------------
    // trigger - validation
    // ------------------------------------------------------------------

    @Test
    void trigger_instanceNotFound_throwsWorkflowNotFound() {
        when(workflowInstanceMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> workflowInstanceService.trigger(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.WORKFLOW_NOT_FOUND.getCode());
    }

    @Test
    void trigger_templateNotFound_throwsWorkflowNotFound() {
        SfWorkflowInstance instance = new SfWorkflowInstance();
        instance.setId(1L);
        instance.setTemplateId(99L);
        when(workflowInstanceMapper.selectById(1L)).thenReturn(instance);
        when(templateMapper.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> workflowInstanceService.trigger(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.WORKFLOW_NOT_FOUND.getCode());
    }

    // ------------------------------------------------------------------
    // trigger - publish gate (spec §4: DRAFT templates are not executable)
    // ------------------------------------------------------------------

    @Test
    void trigger_draftTemplate_rejectedAndInstanceFailed() {
        assertThatThrownBy(() -> triggerWithTemplateStatus("draft"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void trigger_inactiveTemplate_rejected() {
        assertThatThrownBy(() -> triggerWithTemplateStatus("inactive"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void trigger_nullStatusTemplate_rejected() {
        assertThatThrownBy(() -> triggerWithTemplateStatus(null))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void trigger_draftTemplate_marksInstanceFailedInDb() {
        SfWorkflowInstance instance = new SfWorkflowInstance();
        instance.setId(1L);
        instance.setTemplateId(10L);
        instance.setStatus("PENDING");
        when(workflowInstanceMapper.selectById(1L)).thenReturn(instance);

        SfWorkflowTemplate template = new SfWorkflowTemplate();
        template.setId(10L);
        template.setStatus("draft");
        template.setNodeConfigJson("[{\"nodeId\":\"n1\",\"nodeType\":\"AI\",\"input\":{}}]");
        when(templateMapper.selectById(10L)).thenReturn(template);

        assertThatThrownBy(() -> workflowInstanceService.trigger(1L))
                .isInstanceOf(BaseException.class);

        assertThat(instance.getStatus()).isEqualTo("FAILED");
        verify(workflowInstanceMapper).updateById(instance);
        verify(nodeEngine, never()).executeNode(any());
    }

    private Void triggerWithTemplateStatus(String status) {
        SfWorkflowInstance instance = new SfWorkflowInstance();
        instance.setId(1L);
        instance.setTemplateId(10L);
        instance.setStatus("PENDING");
        when(workflowInstanceMapper.selectById(1L)).thenReturn(instance);

        SfWorkflowTemplate template = new SfWorkflowTemplate();
        template.setId(10L);
        template.setStatus(status);
        template.setNodeConfigJson("[{\"nodeId\":\"n1\",\"nodeType\":\"AI\",\"input\":{}}]");
        when(templateMapper.selectById(10L)).thenReturn(template);

        workflowInstanceService.trigger(1L);
        return null;
    }

    // ------------------------------------------------------------------
    // trigger - empty node config
    // ------------------------------------------------------------------

    @Test
    void trigger_emptyNodeConfig_completesImmediately() throws Exception {
        SfWorkflowInstance instance = new SfWorkflowInstance();
        instance.setId(1L);
        instance.setTemplateId(10L);
        instance.setStatus("PENDING");
        when(workflowInstanceMapper.selectById(1L)).thenReturn(instance);

        SfWorkflowTemplate template = new SfWorkflowTemplate();
        template.setStatus("deployed");
        template.setId(10L);
        template.setNodeConfigJson(null);
        when(templateMapper.selectById(10L)).thenReturn(template);

        workflowInstanceService.trigger(1L);

        assertThat(instance.getStatus()).isEqualTo("COMPLETED");
        verify(workflowInstanceMapper, atLeastOnce()).updateById(instance);
        verify(nodeExecutionMapper, never()).insert(any());
    }

    @Test
    void trigger_blankNodeConfig_completesImmediately() throws Exception {
        SfWorkflowInstance instance = new SfWorkflowInstance();
        instance.setId(1L);
        instance.setTemplateId(10L);
        instance.setStatus("PENDING");
        when(workflowInstanceMapper.selectById(1L)).thenReturn(instance);

        SfWorkflowTemplate template = new SfWorkflowTemplate();
        template.setStatus("deployed");
        template.setId(10L);
        template.setNodeConfigJson("  ");
        when(templateMapper.selectById(10L)).thenReturn(template);

        workflowInstanceService.trigger(1L);

        assertThat(instance.getStatus()).isEqualTo("COMPLETED");
    }

    // ------------------------------------------------------------------
    // trigger - node execution
    // ------------------------------------------------------------------

    @Test
    void trigger_allNodesSucceed_setsCompleted() {
        SfWorkflowInstance instance = new SfWorkflowInstance();
        instance.setId(1L);
        instance.setTemplateId(10L);
        instance.setStatus("PENDING");
        when(workflowInstanceMapper.selectById(1L)).thenReturn(instance);

        SfWorkflowTemplate template = new SfWorkflowTemplate();
        template.setStatus("deployed");
        template.setId(10L);
        template.setNodeConfigJson("[{\"nodeId\":\"n1\",\"nodeType\":\"AI\",\"input\":{}}]");
        when(templateMapper.selectById(10L)).thenReturn(template);
        when(nodeExecutionMapper.insert(any())).thenReturn(1);
        when(nodeEngine.executeNode(any())).thenReturn(NodeExecutionResult.success());

        workflowInstanceService.trigger(1L);

        assertThat(instance.getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    void trigger_upstreamOutputInjectedIntoDownstreamInput() {
        SfWorkflowInstance instance = new SfWorkflowInstance();
        instance.setId(1L);
        instance.setTemplateId(10L);
        instance.setStatus("PENDING");
        when(workflowInstanceMapper.selectById(1L)).thenReturn(instance);

        SfWorkflowTemplate template = new SfWorkflowTemplate();
        template.setId(10L);
        template.setStatus("deployed");
        template.setNodeConfigJson("["
                + "{\"nodeId\":\"n1\",\"nodeType\":\"AI_MODEL\",\"input\":{\"prompt\":\"start\"}},"
                + "{\"nodeId\":\"n2\",\"nodeType\":\"AI_MODEL\",\"input\":{\"prompt\":\"use ${input.token} and ${n1.token}\"}}"
                + "]");
        when(templateMapper.selectById(10L)).thenReturn(template);
        when(nodeExecutionMapper.insert(any())).thenReturn(1);
        when(nodeEngine.executeNode(any()))
                .thenReturn(NodeExecutionResult.success(Map.of("token", "abc")))
                .thenReturn(NodeExecutionResult.success());

        workflowInstanceService.trigger(1L);

        org.mockito.ArgumentCaptor<SfWorkflowNodeExecution> captor =
                org.mockito.ArgumentCaptor.forClass(SfWorkflowNodeExecution.class);
        verify(nodeExecutionMapper, times(2)).insert(captor.capture());
        assertThat(captor.getAllValues().get(0).getInputJson()).contains("start");
        assertThat(captor.getAllValues().get(1).getInputJson()).contains("use abc and abc");
        assertThat(instance.getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    void trigger_noPlaceholderResolutionWithoutUpstream_placeholderPreserved() {
        SfWorkflowInstance instance = new SfWorkflowInstance();
        instance.setId(1L);
        instance.setTemplateId(10L);
        instance.setStatus("PENDING");
        when(workflowInstanceMapper.selectById(1L)).thenReturn(instance);

        SfWorkflowTemplate template = new SfWorkflowTemplate();
        template.setId(10L);
        template.setStatus("deployed");
        template.setNodeConfigJson(
                "[{\"nodeId\":\"n1\",\"nodeType\":\"AI_MODEL\",\"input\":{\"prompt\":\"use ${input.missing}\"}}]");
        when(templateMapper.selectById(10L)).thenReturn(template);
        when(nodeExecutionMapper.insert(any())).thenReturn(1);
        when(nodeEngine.executeNode(any())).thenReturn(NodeExecutionResult.success());

        workflowInstanceService.trigger(1L);

        org.mockito.ArgumentCaptor<SfWorkflowNodeExecution> captor =
                org.mockito.ArgumentCaptor.forClass(SfWorkflowNodeExecution.class);
        verify(nodeExecutionMapper).insert(captor.capture());
        assertThat(captor.getValue().getInputJson()).contains("${input.missing}");
    }

    @Test
    void trigger_nodeThrowsException_marksInstanceFailedAndRethrows() {
        SfWorkflowInstance instance = new SfWorkflowInstance();
        instance.setId(1L);
        instance.setTemplateId(10L);
        instance.setStatus("PENDING");
        when(workflowInstanceMapper.selectById(1L)).thenReturn(instance);

        SfWorkflowTemplate template = new SfWorkflowTemplate();
        template.setId(10L);
        template.setStatus("deployed");
        template.setNodeConfigJson("[{\"nodeId\":\"n1\",\"nodeType\":\"AI\",\"input\":{}}]");
        when(templateMapper.selectById(10L)).thenReturn(template);
        when(nodeExecutionMapper.insert(any())).thenReturn(1);
        when(nodeEngine.executeNode(any())).thenThrow(new BaseException(ResultCode.ERROR, "boom"));

        assertThatThrownBy(() -> workflowInstanceService.trigger(1L))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("boom");

        assertThat(instance.getStatus()).isEqualTo("FAILED");
        verify(workflowInstanceMapper, atLeast(2)).updateById(instance);
    }

    @Test
    void trigger_nodeFails_setsFailed() {
        SfWorkflowInstance instance = new SfWorkflowInstance();
        instance.setId(1L);
        instance.setTemplateId(10L);
        instance.setStatus("PENDING");
        when(workflowInstanceMapper.selectById(1L)).thenReturn(instance);

        SfWorkflowTemplate template = new SfWorkflowTemplate();
        template.setStatus("deployed");
        template.setId(10L);
        template.setNodeConfigJson("[{\"nodeId\":\"n1\",\"nodeType\":\"AI\",\"input\":{}}]");
        when(templateMapper.selectById(10L)).thenReturn(template);
        when(nodeExecutionMapper.insert(any())).thenReturn(1);
        when(nodeEngine.executeNode(any())).thenReturn(NodeExecutionResult.failure("Node failed"));

        workflowInstanceService.trigger(1L);

        assertThat(instance.getStatus()).isEqualTo("FAILED");
    }

    @Test
    void trigger_setsStatusToRunningBeforeExecution() throws Exception {
        SfWorkflowInstance instance = new SfWorkflowInstance();
        instance.setId(1L);
        instance.setTemplateId(10L);
        instance.setStatus("PENDING");
        when(workflowInstanceMapper.selectById(1L)).thenReturn(instance);

        SfWorkflowTemplate template = new SfWorkflowTemplate();
        template.setStatus("deployed");
        template.setId(10L);
        template.setNodeConfigJson(null);
        when(templateMapper.selectById(10L)).thenReturn(template);

        workflowInstanceService.trigger(1L);

        // First update sets RUNNING, second sets COMPLETED
        verify(workflowInstanceMapper, atLeast(2)).updateById(instance);
    }

    // ------------------------------------------------------------------
    // trigger - topology hash
    // ------------------------------------------------------------------

    @Test
    void trigger_firstTrigger_storesTopologyHash() {
        SfWorkflowInstance instance = new SfWorkflowInstance();
        instance.setId(1L);
        instance.setTemplateId(10L);
        instance.setStatus("PENDING");
        instance.setTopologyHash(null); // first trigger
        when(workflowInstanceMapper.selectById(1L)).thenReturn(instance);

        String nodeConfig = "[{\"nodeId\":\"n1\",\"nodeType\":\"AI\",\"input\":{}}]";
        SfWorkflowTemplate template = new SfWorkflowTemplate();
        template.setStatus("deployed");
        template.setId(10L);
        template.setNodeConfigJson(nodeConfig);
        when(templateMapper.selectById(10L)).thenReturn(template);
        when(nodeExecutionMapper.insert(any())).thenReturn(1);
        when(nodeEngine.executeNode(any())).thenReturn(NodeExecutionResult.success());

        workflowInstanceService.trigger(1L);

        // Verify topology hash was set
        String expectedHash = TopologyHasher.hash(nodeConfig);
        assertThat(instance.getTopologyHash()).isEqualTo(expectedHash);
    }

    @Test
    void trigger_resumeWithMatchingHash_succeeds() {
        String nodeConfig = "[{\"nodeId\":\"n1\",\"nodeType\":\"AI\",\"input\":{}}]";
        String hash = TopologyHasher.hash(nodeConfig);

        SfWorkflowInstance instance = new SfWorkflowInstance();
        instance.setId(1L);
        instance.setTemplateId(10L);
        instance.setStatus("PENDING");
        instance.setTopologyHash(hash);
        when(workflowInstanceMapper.selectById(1L)).thenReturn(instance);

        SfWorkflowTemplate template = new SfWorkflowTemplate();
        template.setStatus("deployed");
        template.setId(10L);
        template.setNodeConfigJson(nodeConfig);
        when(templateMapper.selectById(10L)).thenReturn(template);
        when(nodeExecutionMapper.insert(any())).thenReturn(1);
        when(nodeEngine.executeNode(any())).thenReturn(NodeExecutionResult.success());

        workflowInstanceService.trigger(1L);

        assertThat(instance.getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    void trigger_resumeWithMismatchedHash_setsFailedAndThrows() {
        String originalConfig = "[{\"nodeId\":\"n1\",\"nodeType\":\"AI\",\"input\":{}}]";
        String originalHash = TopologyHasher.hash(originalConfig);

        SfWorkflowInstance instance = new SfWorkflowInstance();
        instance.setId(1L);
        instance.setTemplateId(10L);
        instance.setStatus("PENDING");
        instance.setTopologyHash(originalHash);
        when(workflowInstanceMapper.selectById(1L)).thenReturn(instance);

        // Template has been modified since checkpoint
        String modifiedConfig = "[{\"nodeId\":\"n2\",\"nodeType\":\"HTTP\",\"input\":{}}]";
        SfWorkflowTemplate template = new SfWorkflowTemplate();
        template.setStatus("deployed");
        template.setId(10L);
        template.setNodeConfigJson(modifiedConfig);
        when(templateMapper.selectById(10L)).thenReturn(template);

        assertThatThrownBy(() -> workflowInstanceService.trigger(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.WORKFLOW_NOT_FOUND.getCode());

        // Instance should be marked as FAILED
        assertThat(instance.getStatus()).isEqualTo("FAILED");
    }

    // ------------------------------------------------------------------
    // trigger - instance input/output and start/end times (spec §5.2)
    // ------------------------------------------------------------------

    @Test
    void trigger_seedsInstanceInputIntoSubstitutionAndPersistsOutput() {
        SfWorkflowInstance instance = new SfWorkflowInstance();
        instance.setId(1L);
        instance.setTemplateId(10L);
        instance.setStatus("PENDING");
        instance.setInputData("{\"topic\":\"alpha\"}");
        when(workflowInstanceMapper.selectById(1L)).thenReturn(instance);

        SfWorkflowTemplate template = new SfWorkflowTemplate();
        template.setId(10L);
        template.setStatus("deployed");
        template.setNodeConfigJson(
                "[{\"nodeId\":\"n1\",\"nodeType\":\"AI_MODEL\",\"input\":{\"prompt\":\"about ${input.topic}\"}}]");
        when(templateMapper.selectById(10L)).thenReturn(template);
        when(nodeExecutionMapper.insert(any())).thenReturn(1);
        when(nodeEngine.executeNode(any())).thenReturn(NodeExecutionResult.success(Map.of("token", "abc")));

        workflowInstanceService.trigger(1L);

        org.mockito.ArgumentCaptor<SfWorkflowNodeExecution> captor =
                org.mockito.ArgumentCaptor.forClass(SfWorkflowNodeExecution.class);
        verify(nodeExecutionMapper).insert(captor.capture());
        assertThat(captor.getValue().getInputJson()).contains("about alpha");

        assertThat(instance.getStatus()).isEqualTo("COMPLETED");
        assertThat(instance.getOutputData()).contains("token").contains("abc");
        assertThat(instance.getStartedAt()).isNotNull();
        assertThat(instance.getCompletedAt()).isNotNull();
    }

    // ------------------------------------------------------------------
    // HUMAN_APPROVAL pause and approve/reject control plane (spec §3.5 / §6.2)
    // ------------------------------------------------------------------

    @Test
    void trigger_humanApprovalNode_pausesInstanceInWaitingApproval() {
        SfWorkflowInstance instance = new SfWorkflowInstance();
        instance.setId(1L);
        instance.setTemplateId(10L);
        instance.setStatus("PENDING");
        instance.setTenantId("t1");
        when(workflowInstanceMapper.selectById(1L)).thenReturn(instance);

        SfWorkflowTemplate template = new SfWorkflowTemplate();
        template.setId(10L);
        template.setStatus("deployed");
        template.setNodeConfigJson("["
                + "{\"nodeId\":\"n1\",\"nodeType\":\"AI_MODEL\",\"input\":{}},"
                + "{\"nodeId\":\"gate\",\"nodeType\":\"HUMAN_APPROVAL\",\"input\":{\"approverRole\":\"lead\"}},"
                + "{\"nodeId\":\"n3\",\"nodeType\":\"AI_MODEL\",\"input\":{}}"
                + "]");
        when(templateMapper.selectById(10L)).thenReturn(template);
        when(nodeExecutionMapper.insert(any())).thenReturn(1);
        when(nodeEngine.executeNode(any())).thenReturn(NodeExecutionResult.success());

        workflowInstanceService.trigger(1L);

        assertThat(instance.getStatus()).isEqualTo("WAITING_APPROVAL");
        // Only n1 ran; n3 waits for the approval decision.
        verify(nodeEngine, times(1)).executeNode(any());

        org.mockito.ArgumentCaptor<SfWorkflowNodeExecution> captor =
                org.mockito.ArgumentCaptor.forClass(SfWorkflowNodeExecution.class);
        verify(nodeExecutionMapper, times(2)).insert(captor.capture());
        SfWorkflowNodeExecution gate = captor.getAllValues().get(1);
        assertThat(gate.getNodeId()).isEqualTo("gate");
        assertThat(gate.getNodeType()).isEqualTo("HUMAN_APPROVAL");
        assertThat(gate.getStatus()).isEqualTo("WAITING_APPROVAL");
        assertThat(gate.getTenantId()).isEqualTo("t1");
    }

    @Test
    void approve_resumesAfterGate_skippingCompletedNodes() {
        SfWorkflowInstance instance = new SfWorkflowInstance();
        instance.setId(1L);
        instance.setTemplateId(10L);
        instance.setStatus("WAITING_APPROVAL");
        instance.setTenantId("t1");
        when(workflowInstanceMapper.selectById(1L)).thenReturn(instance);

        SfWorkflowTemplate template = new SfWorkflowTemplate();
        template.setId(10L);
        template.setStatus("deployed");
        template.setNodeConfigJson("["
                + "{\"nodeId\":\"n1\",\"nodeType\":\"AI_MODEL\",\"input\":{\"prompt\":\"start\"}},"
                + "{\"nodeId\":\"gate\",\"nodeType\":\"HUMAN_APPROVAL\",\"input\":{}},"
                + "{\"nodeId\":\"n3\",\"nodeType\":\"AI_MODEL\",\"input\":{\"prompt\":\"use ${input.token}\"}}"
                + "]");
        when(templateMapper.selectById(10L)).thenReturn(template);

        // Previous leg: n1 completed with output, gate left waiting.
        SfWorkflowNodeExecution completedNode = new SfWorkflowNodeExecution();
        completedNode.setId(101L);
        completedNode.setNodeId("n1");
        completedNode.setStatus("COMPLETED");
        completedNode.setOutputJson("{\"token\":\"abc\"}");
        SfWorkflowNodeExecution gateNode = new SfWorkflowNodeExecution();
        gateNode.setId(102L);
        gateNode.setNodeId("gate");
        gateNode.setStatus("WAITING_APPROVAL");
        // 1st query: findWaitingApprovalNode (filtered on WAITING_APPROVAL).
        // 2nd query: resume reconstruction, after approve() flipped the gate to COMPLETED.
        when(nodeExecutionMapper.selectList(any()))
                .thenReturn(java.util.List.of(gateNode))
                .thenReturn(java.util.List.of(completedNode, gateNode));
        when(nodeExecutionMapper.updateById(any())).thenReturn(1);
        when(nodeExecutionMapper.insert(any())).thenReturn(1);
        when(nodeEngine.executeNode(any())).thenReturn(NodeExecutionResult.success());

        workflowInstanceService.approve(1L, "looks good");

        // The gate decision is recorded on the node execution...
        assertThat(gateNode.getStatus()).isEqualTo("COMPLETED");
        assertThat(gateNode.getOutputJson()).contains("approved").contains("true").contains("looks good");

        // ...and execution resumes at n3 only, with upstream outputs re-seeded.
        assertThat(instance.getStatus()).isEqualTo("COMPLETED");
        org.mockito.ArgumentCaptor<SfWorkflowNodeExecution> engineCaptor =
                org.mockito.ArgumentCaptor.forClass(SfWorkflowNodeExecution.class);
        verify(nodeEngine, times(1)).executeNode(engineCaptor.capture());
        assertThat(engineCaptor.getValue().getNodeId()).isEqualTo("n3");
        assertThat(engineCaptor.getValue().getInputJson()).contains("use abc");
        // n1 and the gate are not re-inserted.
        verify(nodeExecutionMapper, times(1)).insert(any());
    }

    @Test
    void approve_notWaitingApproval_throwsConflict() {
        SfWorkflowInstance instance = new SfWorkflowInstance();
        instance.setId(1L);
        instance.setStatus("RUNNING");
        when(workflowInstanceMapper.selectById(1L)).thenReturn(instance);

        assertThatThrownBy(() -> workflowInstanceService.approve(1L, "ok"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.CONFLICT.getCode());

        verify(nodeEngine, never()).executeNode(any());
    }

    @Test
    void reject_marksGateFailedAndFailsInstance() {
        SfWorkflowInstance instance = new SfWorkflowInstance();
        instance.setId(1L);
        instance.setTemplateId(10L);
        instance.setStatus("WAITING_APPROVAL");
        when(workflowInstanceMapper.selectById(1L)).thenReturn(instance);

        SfWorkflowNodeExecution gateNode = new SfWorkflowNodeExecution();
        gateNode.setId(102L);
        gateNode.setNodeId("gate");
        gateNode.setStatus("WAITING_APPROVAL");
        when(nodeExecutionMapper.selectList(any())).thenReturn(java.util.List.of(gateNode));
        when(nodeExecutionMapper.updateById(any())).thenReturn(1);

        workflowInstanceService.reject(1L, "too risky");

        assertThat(gateNode.getStatus()).isEqualTo("FAILED");
        assertThat(gateNode.getOutputJson()).contains("approved").contains("false").contains("too risky");
        assertThat(instance.getStatus()).isEqualTo("FAILED");
        assertThat(instance.getCompletedAt()).isNotNull();
        verify(nodeEngine, never()).executeNode(any());
    }

    @Test
    void reject_notWaitingApproval_throwsConflict() {
        SfWorkflowInstance instance = new SfWorkflowInstance();
        instance.setId(1L);
        instance.setStatus("COMPLETED");
        when(workflowInstanceMapper.selectById(1L)).thenReturn(instance);

        assertThatThrownBy(() -> workflowInstanceService.reject(1L, "no"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.CONFLICT.getCode());
    }

    // ------------------------------------------------------------------
    // cancel (spec §6.2 / §4: CANCELLED reachable)
    // ------------------------------------------------------------------

    @Test
    void cancel_runningInstance_setsCancelled() {
        SfWorkflowInstance instance = new SfWorkflowInstance();
        instance.setId(1L);
        instance.setStatus("RUNNING");
        when(workflowInstanceMapper.selectById(1L)).thenReturn(instance);

        workflowInstanceService.cancel(1L);

        assertThat(instance.getStatus()).isEqualTo("CANCELLED");
        assertThat(instance.getCompletedAt()).isNotNull();
        verify(workflowInstanceMapper).updateById(instance);
    }

    @Test
    void cancel_waitingApprovalInstance_setsCancelled() {
        SfWorkflowInstance instance = new SfWorkflowInstance();
        instance.setId(1L);
        instance.setStatus("WAITING_APPROVAL");
        when(workflowInstanceMapper.selectById(1L)).thenReturn(instance);

        workflowInstanceService.cancel(1L);

        assertThat(instance.getStatus()).isEqualTo("CANCELLED");
    }

    @Test
    void cancel_completedInstance_throwsConflict() {
        SfWorkflowInstance instance = new SfWorkflowInstance();
        instance.setId(1L);
        instance.setStatus("COMPLETED");
        when(workflowInstanceMapper.selectById(1L)).thenReturn(instance);

        assertThatThrownBy(() -> workflowInstanceService.cancel(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.CONFLICT.getCode());

        verify(workflowInstanceMapper, never()).updateById(any());
    }

    @Test
    void cancel_instanceNotFound_throwsWorkflowNotFound() {
        when(workflowInstanceMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> workflowInstanceService.cancel(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.WORKFLOW_NOT_FOUND.getCode());
    }

    @Test
    void trigger_cancellationAtNodeBoundary_stopsBeforeNextNode() {
        SfWorkflowInstance instance = new SfWorkflowInstance();
        instance.setId(1L);
        instance.setTemplateId(10L);
        instance.setStatus("PENDING");
        SfWorkflowInstance cancelled = new SfWorkflowInstance();
        cancelled.setId(1L);
        cancelled.setStatus("CANCELLED");

        // 1st read: trigger start; 2nd: boundary check before n1; 3rd: boundary check
        // before n2 observes the concurrent cancel and stops the loop.
        when(workflowInstanceMapper.selectById(1L)).thenReturn(instance, instance, cancelled);

        SfWorkflowTemplate template = new SfWorkflowTemplate();
        template.setId(10L);
        template.setStatus("deployed");
        template.setNodeConfigJson("["
                + "{\"nodeId\":\"n1\",\"nodeType\":\"AI_MODEL\",\"input\":{}},"
                + "{\"nodeId\":\"n2\",\"nodeType\":\"AI_MODEL\",\"input\":{}}"
                + "]");
        when(templateMapper.selectById(10L)).thenReturn(template);
        when(nodeExecutionMapper.insert(any())).thenReturn(1);
        when(nodeEngine.executeNode(any())).thenReturn(NodeExecutionResult.success());

        workflowInstanceService.trigger(1L);

        verify(nodeEngine, times(1)).executeNode(any());
        assertThat(cancelled.getStatus()).isEqualTo("CANCELLED");
        assertThat(instance.getStatus()).isNotEqualTo("COMPLETED");
    }
}
