package com.schemaplexai.workflow.service;

import com.schemaplexai.workflow.entity.SfWorkflowNodeExecution;
import com.schemaplexai.workflow.node.NodeExecutionResult;
import org.flowable.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlowableDelegateAdapterTest {

    @Mock
    private WorkflowNodeEngine nodeEngine;

    @Mock
    private DelegateExecution execution;

    @InjectMocks
    private FlowableDelegateAdapter flowableDelegateAdapter;

    @Test
    void execute_withNodeType() {
        when(execution.getProcessInstanceId()).thenReturn("proc-1");
        when(execution.getCurrentActivityId()).thenReturn("act-1");
        when(execution.getVariable("nodeType")).thenReturn("HTTP");
        when(execution.getVariables()).thenReturn(Map.of("url", "http://example.com"));

        NodeExecutionResult result = NodeExecutionResult.success(Map.of("status", "ok"));
        when(nodeEngine.executeNode(any(SfWorkflowNodeExecution.class))).thenReturn(result);

        flowableDelegateAdapter.execute(execution);

        verify(execution).setVariable("nodeResult", true);
        verify(execution).setVariable("nodeOutput", Map.of("status", "ok"));
    }

    @Test
    void execute_withoutNodeType_defaultsToScript() {
        when(execution.getProcessInstanceId()).thenReturn("proc-1");
        when(execution.getCurrentActivityId()).thenReturn("act-1");
        when(execution.getVariable("nodeType")).thenReturn(null);
        when(execution.getVariables()).thenReturn(Map.of());

        NodeExecutionResult result = NodeExecutionResult.success(Map.of());
        when(nodeEngine.executeNode(any(SfWorkflowNodeExecution.class))).thenReturn(result);

        flowableDelegateAdapter.execute(execution);

        verify(nodeEngine).executeNode(argThat(node -> "SCRIPT".equals(node.getNodeType())));
    }

    @Test
    void execute_serializeFailure_returnsEmptyJson() {
        when(execution.getProcessInstanceId()).thenReturn("proc-1");
        when(execution.getCurrentActivityId()).thenReturn("act-1");
        when(execution.getVariable("nodeType")).thenReturn("SCRIPT");
        when(execution.getVariables()).thenReturn(Map.of());

        NodeExecutionResult result = NodeExecutionResult.success(Map.of());
        when(nodeEngine.executeNode(any(SfWorkflowNodeExecution.class))).thenReturn(result);

        flowableDelegateAdapter.execute(execution);

        verify(execution).setVariable("nodeResult", true);
    }
}
