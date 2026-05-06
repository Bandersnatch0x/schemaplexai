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
        template.setId(10L);
        template.setNodeConfigJson("[{\"nodeId\":\"n1\",\"nodeType\":\"AI\",\"input\":{}}]");
        when(templateMapper.selectById(10L)).thenReturn(template);
        when(nodeExecutionMapper.insert(any())).thenReturn(1);
        when(nodeEngine.executeNode(any())).thenReturn(NodeExecutionResult.success());

        workflowInstanceService.trigger(1L);

        assertThat(instance.getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    void trigger_nodeFails_setsFailed() {
        SfWorkflowInstance instance = new SfWorkflowInstance();
        instance.setId(1L);
        instance.setTemplateId(10L);
        instance.setStatus("PENDING");
        when(workflowInstanceMapper.selectById(1L)).thenReturn(instance);

        SfWorkflowTemplate template = new SfWorkflowTemplate();
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
        template.setId(10L);
        template.setNodeConfigJson(null);
        when(templateMapper.selectById(10L)).thenReturn(template);

        workflowInstanceService.trigger(1L);

        // First update sets RUNNING, second sets COMPLETED
        verify(workflowInstanceMapper, atLeast(2)).updateById(instance);
    }
}
