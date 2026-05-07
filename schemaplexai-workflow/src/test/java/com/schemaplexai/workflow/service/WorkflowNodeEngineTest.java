package com.schemaplexai.workflow.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.workflow.entity.SfWorkflowNodeExecution;
import com.schemaplexai.workflow.mapper.SfWorkflowNodeExecutionMapper;
import com.schemaplexai.workflow.node.NodeExecutionResult;
import com.schemaplexai.workflow.node.NodeExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkflowNodeEngineTest {

    @Mock
    private NodeExecutor nodeExecutor;

    @Mock
    private SfWorkflowNodeExecutionMapper nodeExecutionMapper;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private WorkflowNodeEngine workflowNodeEngine;

    @BeforeEach
    void init() {
        when(nodeExecutor.getNodeType()).thenReturn("SCRIPT");
        ReflectionTestUtils.setField(workflowNodeEngine, "executorList", List.of(nodeExecutor));
        workflowNodeEngine.init();
    }

    @Test
    void executeNode_success() throws Exception {
        SfWorkflowNodeExecution node = new SfWorkflowNodeExecution();
        node.setNodeId("n1");
        node.setNodeType("SCRIPT");
        node.setInputJson("{\"key\":\"val\"}");
        node.setTenantId("t1");

        when(objectMapper.readValue(eq("{\"key\":\"val\"}"), any(TypeReference.class)))
                .thenReturn(Map.of("key", "val"));
        when(nodeExecutor.execute(any(), eq("t1")))
                .thenReturn(NodeExecutionResult.success(Map.of("result", "ok")));
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"result\":\"ok\"}");
        when(nodeExecutionMapper.updateById(any())).thenReturn(1);

        NodeExecutionResult result = workflowNodeEngine.executeNode(node);

        assertThat(result.isSuccess()).isTrue();
        assertThat(node.getStatus()).isEqualTo("COMPLETED");
        verify(nodeExecutionMapper, times(2)).updateById(any());
    }

    @Test
    void executeNode_failureResult() throws Exception {
        SfWorkflowNodeExecution node = new SfWorkflowNodeExecution();
        node.setNodeId("n1");
        node.setNodeType("SCRIPT");
        node.setInputJson(null);
        node.setTenantId("t1");

        when(nodeExecutor.execute(any(), eq("t1")))
                .thenReturn(NodeExecutionResult.failure("Error"));
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"error\":\"Error\"}");
        when(nodeExecutionMapper.updateById(any())).thenReturn(1);

        NodeExecutionResult result = workflowNodeEngine.executeNode(node);

        assertThat(result.isSuccess()).isFalse();
        assertThat(node.getStatus()).isEqualTo("FAILED");
    }

    @Test
    void executeNode_unknownType_throwsException() {
        SfWorkflowNodeExecution node = new SfWorkflowNodeExecution();
        node.setNodeId("n1");
        node.setNodeType("UNKNOWN");

        assertThatThrownBy(() -> workflowNodeEngine.executeNode(node))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("No executor for node type");
    }

    @Test
    void executeNode_exception_setsFailedStatus() throws Exception {
        SfWorkflowNodeExecution node = new SfWorkflowNodeExecution();
        node.setNodeId("n1");
        node.setNodeType("SCRIPT");
        node.setInputJson("{\"key\":\"val\"}");
        node.setTenantId("t1");

        when(objectMapper.readValue(eq("{\"key\":\"val\"}"), any(TypeReference.class)))
                .thenReturn(Map.of("key", "val"));
        when(nodeExecutor.execute(any(), eq("t1")))
                .thenThrow(new RuntimeException("Boom"));
        when(nodeExecutionMapper.updateById(any())).thenReturn(1);

        assertThatThrownBy(() -> workflowNodeEngine.executeNode(node))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("Boom");
        assertThat(node.getStatus()).isEqualTo("FAILED");
    }

    @Test
    void executeNode_invalidInputJson_usesEmptyMap() throws Exception {
        SfWorkflowNodeExecution node = new SfWorkflowNodeExecution();
        node.setNodeId("n1");
        node.setNodeType("SCRIPT");
        node.setInputJson("bad json");
        node.setTenantId("t1");

        when(objectMapper.readValue(eq("bad json"), any(TypeReference.class)))
                .thenThrow(new com.fasterxml.jackson.core.JsonProcessingException("bad") {});
        when(nodeExecutor.execute(any(), eq("t1")))
                .thenReturn(NodeExecutionResult.success(Map.of()));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
        when(nodeExecutionMapper.updateById(any())).thenReturn(1);

        NodeExecutionResult result = workflowNodeEngine.executeNode(node);

        assertThat(result.isSuccess()).isTrue();
    }
}
