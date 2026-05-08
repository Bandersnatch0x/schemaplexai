package com.schemaplexai.workflow.service;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.repository.ProcessDefinitionQuery;
import org.flowable.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkflowDeployServiceTest {

    @Mock
    private RepositoryService repositoryService;

    @Mock
    private RuntimeService runtimeService;

    @InjectMocks
    private WorkflowDeployService workflowDeployService;

    // ------------------------------------------------------------------
    // listDeployedProcesses
    // ------------------------------------------------------------------

    @Test
    void listDeployedProcesses_returnsActiveDefinitions() {
        ProcessDefinition def = mock(ProcessDefinition.class);
        when(def.getId()).thenReturn("id1");
        when(def.getKey()).thenReturn("key1");
        when(def.getName()).thenReturn("Process1");
        when(def.getVersion()).thenReturn(1);
        when(def.getDeploymentId()).thenReturn("dep1");
        when(def.isSuspended()).thenReturn(false);

        ProcessDefinitionQuery query = mock(ProcessDefinitionQuery.class);
        when(repositoryService.createProcessDefinitionQuery()).thenReturn(query);
        when(query.active()).thenReturn(query);
        when(query.orderByProcessDefinitionKey()).thenReturn(query);
        when(query.asc()).thenReturn(query);
        when(query.list()).thenReturn(List.of(def));

        List<WorkflowDeployService.ProcessDefinitionInfo> result = workflowDeployService.listDeployedProcesses();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).key()).isEqualTo("key1");
        assertThat(result.get(0).suspended()).isFalse();
    }

    @Test
    void listDeployedProcesses_empty_returnsEmptyList() {
        ProcessDefinitionQuery query = mock(ProcessDefinitionQuery.class);
        when(repositoryService.createProcessDefinitionQuery()).thenReturn(query);
        when(query.active()).thenReturn(query);
        when(query.orderByProcessDefinitionKey()).thenReturn(query);
        when(query.asc()).thenReturn(query);
        when(query.list()).thenReturn(List.of());

        List<WorkflowDeployService.ProcessDefinitionInfo> result = workflowDeployService.listDeployedProcesses();

        assertThat(result).isEmpty();
    }

    // ------------------------------------------------------------------
    // startProcessInstance
    // ------------------------------------------------------------------

    @Test
    void startProcessInstance_withBusinessKey_returnsInstanceId() {
        ProcessDefinition def = mock(ProcessDefinition.class);
        ProcessDefinitionQuery query = mock(ProcessDefinitionQuery.class);
        when(repositoryService.createProcessDefinitionQuery()).thenReturn(query);
        when(query.processDefinitionKey("key1")).thenReturn(query);
        when(query.active()).thenReturn(query);
        when(query.latestVersion()).thenReturn(query);
        when(query.singleResult()).thenReturn(def);

        ProcessInstance instance = mock(ProcessInstance.class);
        when(instance.getId()).thenReturn("inst-1");
        when(runtimeService.startProcessInstanceByKey(eq("key1"), eq("biz"), any()))
                .thenReturn(instance);

        String result = workflowDeployService.startProcessInstance("key1", "biz", Map.of());

        assertThat(result).isEqualTo("inst-1");
    }

    @Test
    void startProcessInstance_withoutBusinessKey_returnsInstanceId() {
        ProcessDefinition def = mock(ProcessDefinition.class);
        ProcessDefinitionQuery query = mock(ProcessDefinitionQuery.class);
        when(repositoryService.createProcessDefinitionQuery()).thenReturn(query);
        when(query.processDefinitionKey("key1")).thenReturn(query);
        when(query.active()).thenReturn(query);
        when(query.latestVersion()).thenReturn(query);
        when(query.singleResult()).thenReturn(def);

        ProcessInstance instance = mock(ProcessInstance.class);
        when(instance.getId()).thenReturn("inst-2");
        when(runtimeService.startProcessInstanceByKey(eq("key1"), any(Map.class)))
                .thenReturn(instance);

        String result = workflowDeployService.startProcessInstance("key1", null, Map.of());

        assertThat(result).isEqualTo("inst-2");
    }

    @Test
    void startProcessInstance_definitionNotFound_throwsWorkflowNotFound() {
        ProcessDefinitionQuery query = mock(ProcessDefinitionQuery.class);
        when(repositoryService.createProcessDefinitionQuery()).thenReturn(query);
        when(query.processDefinitionKey("key1")).thenReturn(query);
        when(query.active()).thenReturn(query);
        when(query.latestVersion()).thenReturn(query);
        when(query.singleResult()).thenReturn(null);

        assertThatThrownBy(() -> workflowDeployService.startProcessInstance("key1", "biz", Map.of()))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.WORKFLOW_NOT_FOUND.getCode());
    }

    // ------------------------------------------------------------------
    // suspendProcessDefinition
    // ------------------------------------------------------------------

    @Test
    void suspendProcessDefinition_success() {
        ProcessDefinition def = mock(ProcessDefinition.class);
        when(def.getId()).thenReturn("id1");

        ProcessDefinitionQuery query = mock(ProcessDefinitionQuery.class);
        when(repositoryService.createProcessDefinitionQuery()).thenReturn(query);
        when(query.processDefinitionKey("key1")).thenReturn(query);
        when(query.active()).thenReturn(query);
        when(query.latestVersion()).thenReturn(query);
        when(query.singleResult()).thenReturn(def);

        workflowDeployService.suspendProcessDefinition("key1");

        verify(repositoryService).suspendProcessDefinitionById("id1");
    }

    @Test
    void suspendProcessDefinition_notFound_throwsWorkflowNotFound() {
        ProcessDefinitionQuery query = mock(ProcessDefinitionQuery.class);
        when(repositoryService.createProcessDefinitionQuery()).thenReturn(query);
        when(query.processDefinitionKey("key1")).thenReturn(query);
        when(query.active()).thenReturn(query);
        when(query.latestVersion()).thenReturn(query);
        when(query.singleResult()).thenReturn(null);

        assertThatThrownBy(() -> workflowDeployService.suspendProcessDefinition("key1"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.WORKFLOW_NOT_FOUND.getCode());
    }

    // ------------------------------------------------------------------
    // activateProcessDefinition
    // ------------------------------------------------------------------

    @Test
    void activateProcessDefinition_success() {
        ProcessDefinition def = mock(ProcessDefinition.class);
        when(def.getId()).thenReturn("id1");

        ProcessDefinitionQuery query = mock(ProcessDefinitionQuery.class);
        when(repositoryService.createProcessDefinitionQuery()).thenReturn(query);
        when(query.processDefinitionKey("key1")).thenReturn(query);
        when(query.suspended()).thenReturn(query);
        when(query.latestVersion()).thenReturn(query);
        when(query.singleResult()).thenReturn(def);

        workflowDeployService.activateProcessDefinition("key1");

        verify(repositoryService).activateProcessDefinitionById("id1");
    }

    @Test
    void activateProcessDefinition_notFound_throwsWorkflowNotFound() {
        ProcessDefinitionQuery query = mock(ProcessDefinitionQuery.class);
        when(repositoryService.createProcessDefinitionQuery()).thenReturn(query);
        when(query.processDefinitionKey("key1")).thenReturn(query);
        when(query.suspended()).thenReturn(query);
        when(query.latestVersion()).thenReturn(query);
        when(query.singleResult()).thenReturn(null);

        assertThatThrownBy(() -> workflowDeployService.activateProcessDefinition("key1"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.WORKFLOW_NOT_FOUND.getCode());
    }
}
