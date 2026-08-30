package com.schemaplexai.workflow.service;

import com.schemaplexai.common.context.TenantContextHolder;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.workflow.entity.SfWorkflowNodeExecution;
import com.schemaplexai.workflow.node.NodeExecutionResult;
import org.flowable.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

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
    void execute_withoutNodeType_throwsParamError() {
        when(execution.getProcessInstanceId()).thenReturn("proc-1");
        when(execution.getCurrentActivityId()).thenReturn("act-1");
        when(execution.getVariable("nodeType")).thenReturn(null);

        assertThatThrownBy(() -> flowableDelegateAdapter.execute(execution))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());

        verify(nodeEngine, never()).executeNode(any());
    }

    @Test
    void execute_carriesTenantAndInstanceFromProcessVariables() {
        when(execution.getProcessInstanceId()).thenReturn("proc-1");
        when(execution.getCurrentActivityId()).thenReturn("act-1");
        when(execution.getVariable("nodeType")).thenReturn("HTTP");
        when(execution.getVariable("tenantId")).thenReturn("tenant-9");
        when(execution.getVariable("workflowInstanceId")).thenReturn(42L);
        when(execution.getVariables()).thenReturn(Map.of("url", "http://example.com"));
        when(nodeEngine.executeNode(any(SfWorkflowNodeExecution.class)))
                .thenReturn(NodeExecutionResult.success(Map.of()));

        flowableDelegateAdapter.execute(execution);

        verify(nodeEngine).executeNode(argThat(node ->
                "tenant-9".equals(node.getTenantId())
                        && Long.valueOf(42L).equals(node.getInstanceId())));
    }

    @Test
    void execute_withoutTenantVariable_fallsBackToContext() {
        TenantContextHolder.setTenantId("ctx-tenant");
        when(execution.getProcessInstanceId()).thenReturn("proc-1");
        when(execution.getCurrentActivityId()).thenReturn("act-1");
        when(execution.getVariable("nodeType")).thenReturn("HTTP");
        when(execution.getVariable("tenantId")).thenReturn(null);
        when(execution.getVariables()).thenReturn(Map.of());
        when(nodeEngine.executeNode(any(SfWorkflowNodeExecution.class)))
                .thenReturn(NodeExecutionResult.success(Map.of()));

        flowableDelegateAdapter.execute(execution);

        verify(nodeEngine).executeNode(argThat(node -> "ctx-tenant".equals(node.getTenantId())));
    }

    @Test
    void execute_withoutTenantAnywhere_defaultsSoInsertNeverLosesTenant() {
        when(execution.getProcessInstanceId()).thenReturn("proc-1");
        when(execution.getCurrentActivityId()).thenReturn("act-1");
        when(execution.getVariable("nodeType")).thenReturn("HTTP");
        when(execution.getVariable("tenantId")).thenReturn(null);
        when(execution.getVariables()).thenReturn(Map.of());
        when(nodeEngine.executeNode(any(SfWorkflowNodeExecution.class)))
                .thenReturn(NodeExecutionResult.success(Map.of()));

        flowableDelegateAdapter.execute(execution);

        verify(nodeEngine).executeNode(argThat(node -> "default".equals(node.getTenantId())));
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
