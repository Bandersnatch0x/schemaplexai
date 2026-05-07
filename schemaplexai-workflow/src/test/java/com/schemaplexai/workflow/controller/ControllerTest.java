package com.schemaplexai.workflow.controller;

import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.workflow.entity.SfWorkflowInstance;
import com.schemaplexai.workflow.entity.SfWorkflowTemplate;
import com.schemaplexai.workflow.service.WorkflowDeployService;
import com.schemaplexai.workflow.service.WorkflowInstanceService;
import com.schemaplexai.workflow.service.WorkflowTemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ControllerTest {

    @Mock
    private WorkflowInstanceService workflowInstanceService;

    @Mock
    private WorkflowTemplateService workflowTemplateService;

    @Mock
    private WorkflowDeployService workflowDeployService;

    @InjectMocks
    private WorkflowInstanceController workflowInstanceController;

    @InjectMocks
    private WorkflowTemplateController workflowTemplateController;

    @InjectMocks
    private WorkflowBpmnController workflowBpmnController;

    private SfWorkflowInstance instance;
    private SfWorkflowTemplate template;

    @BeforeEach
    void setUp() {
        instance = new SfWorkflowInstance();
        instance.setId(1L);
        instance.setTemplateId(1L);
        instance.setStatus("ACTIVE");

        template = new SfWorkflowTemplate();
        template.setId(1L);
        template.setName("Test Workflow");
        template.setStatus("ACTIVE");
    }

    // ========== WorkflowInstanceController ==========

    @Test
    void instanceCreate_returnsId() {
        when(workflowInstanceService.save(any())).thenReturn(true);

        Result<Long> result = workflowInstanceController.create(instance);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEqualTo(1L);
    }

    @Test
    void instanceUpdate_returnsBoolean() {
        when(workflowInstanceService.updateById(any())).thenReturn(true);

        Result<Boolean> result = workflowInstanceController.update(1L, instance);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isTrue();
    }

    @Test
    void instanceDelete_returnsBoolean() {
        when(workflowInstanceService.removeById(1L)).thenReturn(true);

        Result<Boolean> result = workflowInstanceController.delete(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isTrue();
    }

    @Test
    void instanceGet_found() {
        when(workflowInstanceService.getById(1L)).thenReturn(instance);

        Result<SfWorkflowInstance> result = workflowInstanceController.get(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getId()).isEqualTo(1L);
    }

    @Test
    void instanceGet_notFound() {
        when(workflowInstanceService.getById(1L)).thenReturn(null);

        Result<SfWorkflowInstance> result = workflowInstanceController.get(1L);

        assertThat(result.getCode()).isEqualTo(404);
    }

    @Test
    void instanceTrigger_returnsSuccess() {
        doNothing().when(workflowInstanceService).trigger(1L);

        Result<Boolean> result = workflowInstanceController.trigger(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isTrue();
    }

    // ========== WorkflowTemplateController ==========

    @Test
    void templateCreate_returnsId() {
        when(workflowTemplateService.save(any())).thenReturn(true);

        Result<Long> result = workflowTemplateController.create(template);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEqualTo(1L);
    }

    @Test
    void templateUpdate_returnsBoolean() {
        when(workflowTemplateService.updateById(any())).thenReturn(true);

        Result<Boolean> result = workflowTemplateController.update(1L, template);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isTrue();
    }

    @Test
    void templateDelete_returnsBoolean() {
        when(workflowTemplateService.removeById(1L)).thenReturn(true);

        Result<Boolean> result = workflowTemplateController.delete(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isTrue();
    }

    @Test
    void templateGet_found() {
        when(workflowTemplateService.getById(1L)).thenReturn(template);

        Result<SfWorkflowTemplate> result = workflowTemplateController.get(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getName()).isEqualTo("Test Workflow");
    }

    @Test
    void templateGet_notFound() {
        when(workflowTemplateService.getById(1L)).thenReturn(null);

        Result<SfWorkflowTemplate> result = workflowTemplateController.get(1L);

        assertThat(result.getCode()).isEqualTo(404);
    }

    @Test
    void templateDeploy() {
        when(workflowTemplateService.deployTemplate(1L)).thenReturn(template);

        Result<SfWorkflowTemplate> result = workflowTemplateController.deployTemplate(1L);

        assertThat(result.getCode()).isEqualTo(200);
    }

    @Test
    void templateValidate() {
        when(workflowTemplateService.validateTemplate(1L)).thenReturn(true);

        Result<Boolean> result = workflowTemplateController.validateTemplate(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isTrue();
    }

    @Test
    void templateClone() {
        when(workflowTemplateService.cloneTemplate(1L, "Clone")).thenReturn(template);

        Result<SfWorkflowTemplate> result = workflowTemplateController.cloneTemplate(1L, "Clone");

        assertThat(result.getCode()).isEqualTo(200);
    }

    @Test
    void templateListDeployed() {
        when(workflowTemplateService.listDeployedTemplates()).thenReturn(List.of(template));

        Result<List<SfWorkflowTemplate>> result = workflowTemplateController.listDeployedTemplates();

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).hasSize(1);
    }

    @Test
    void templateDeactivate() {
        when(workflowTemplateService.deactivateTemplate(1L)).thenReturn(template);

        Result<SfWorkflowTemplate> result = workflowTemplateController.deactivateTemplate(1L);

        assertThat(result.getCode()).isEqualTo(200);
    }

    // ========== WorkflowBpmnController ==========

    @Test
    void bpmnListDeployedProcesses() {
        List<WorkflowDeployService.ProcessDefinitionInfo> list = List.of(
                new WorkflowDeployService.ProcessDefinitionInfo("id1", "key1", "Name", 1, "dep1", false)
        );
        when(workflowDeployService.listDeployedProcesses()).thenReturn(list);

        Result<List<WorkflowDeployService.ProcessDefinitionInfo>> result = workflowBpmnController.listDeployedProcesses();

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).hasSize(1);
    }

    @Test
    void bpmnStartProcess_success() {
        when(workflowDeployService.startProcessInstance("key1", "biz", null)).thenReturn("inst-1");

        Result<String> result = workflowBpmnController.startProcessInstance("key1",
                new WorkflowBpmnController.StartProcessRequest("biz", null));

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEqualTo("inst-1");
    }

    @Test
    void bpmnStartProcess_failure() {
        when(workflowDeployService.startProcessInstance("key1", null, null))
                .thenThrow(new RuntimeException("not found"));

        Result<String> result = workflowBpmnController.startProcessInstance("key1",
                new WorkflowBpmnController.StartProcessRequest(null, null));

        assertThat(result.getCode()).isEqualTo(ResultCode.WORKFLOW_NOT_FOUND.getCode());
    }

    @Test
    void bpmnSuspend() {
        doNothing().when(workflowDeployService).suspendProcessDefinition("key1");

        Result<Boolean> result = workflowBpmnController.suspendProcessDefinition("key1");

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isTrue();
    }

    @Test
    void bpmnActivate() {
        doNothing().when(workflowDeployService).activateProcessDefinition("key1");

        Result<Boolean> result = workflowBpmnController.activateProcessDefinition("key1");

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isTrue();
    }
}
