package com.schemaplexai.workflow.service;

import com.schemaplexai.common.context.TenantContextHolder;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.workflow.entity.SfWorkflowTemplate;
import com.schemaplexai.workflow.mapper.SfWorkflowTemplateMapper;
import com.schemaplexai.workflow.service.impl.WorkflowTemplateServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkflowTemplateServiceImplTest {

    @Mock
    private SfWorkflowTemplateMapper workflowTemplateMapper;

    @InjectMocks
    private WorkflowTemplateServiceImpl workflowTemplateService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(workflowTemplateService, "baseMapper", workflowTemplateMapper);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    // ------------------------------------------------------------------
    // deployTemplate
    // ------------------------------------------------------------------

    @Test
    void deployTemplate_notFound_throwsWorkflowNotFound() {
        when(workflowTemplateMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> workflowTemplateService.deployTemplate(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.WORKFLOW_NOT_FOUND.getCode());
    }

    @Test
    void deployTemplate_alreadyDeployed_throwsParamError() {
        SfWorkflowTemplate template = new SfWorkflowTemplate();
        template.setId(1L);
        template.setStatus("deployed");
        when(workflowTemplateMapper.selectById(1L)).thenReturn(template);

        assertThatThrownBy(() -> workflowTemplateService.deployTemplate(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void deployTemplate_success_setsStatusToDeployed() {
        SfWorkflowTemplate template = new SfWorkflowTemplate();
        template.setId(1L);
        template.setStatus("draft");
        when(workflowTemplateMapper.selectById(1L)).thenReturn(template);

        SfWorkflowTemplate result = workflowTemplateService.deployTemplate(1L);

        assertThat(result.getStatus()).isEqualTo("deployed");
        verify(workflowTemplateMapper).updateById(template);
    }

    @Test
    void deployTemplate_draftStatus_deploysSuccessfully() {
        SfWorkflowTemplate template = new SfWorkflowTemplate();
        template.setId(1L);
        template.setName("Test Workflow");
        template.setStatus("draft");
        when(workflowTemplateMapper.selectById(1L)).thenReturn(template);

        SfWorkflowTemplate result = workflowTemplateService.deployTemplate(1L);

        assertThat(result.getStatus()).isEqualTo("deployed");
    }

    // ------------------------------------------------------------------
    // validateTemplate
    // ------------------------------------------------------------------

    @Test
    void validateTemplate_notFound_throwsWorkflowNotFound() {
        when(workflowTemplateMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> workflowTemplateService.validateTemplate(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.WORKFLOW_NOT_FOUND.getCode());
    }

    @Test
    void validateTemplate_missingNodeConfig_returnsFalse() {
        SfWorkflowTemplate template = new SfWorkflowTemplate();
        template.setId(1L);
        template.setName("Valid Name");
        template.setNodeConfigJson(null);
        when(workflowTemplateMapper.selectById(1L)).thenReturn(template);

        boolean result = workflowTemplateService.validateTemplate(1L);

        assertThat(result).isFalse();
    }

    @Test
    void validateTemplate_blankNodeConfig_returnsFalse() {
        SfWorkflowTemplate template = new SfWorkflowTemplate();
        template.setId(1L);
        template.setName("Valid Name");
        template.setNodeConfigJson("   ");
        when(workflowTemplateMapper.selectById(1L)).thenReturn(template);

        boolean result = workflowTemplateService.validateTemplate(1L);

        assertThat(result).isFalse();
    }

    @Test
    void validateTemplate_missingName_returnsFalse() {
        SfWorkflowTemplate template = new SfWorkflowTemplate();
        template.setId(1L);
        template.setName(null);
        template.setNodeConfigJson("{}");
        when(workflowTemplateMapper.selectById(1L)).thenReturn(template);

        boolean result = workflowTemplateService.validateTemplate(1L);

        assertThat(result).isFalse();
    }

    @Test
    void validateTemplate_blankName_returnsFalse() {
        SfWorkflowTemplate template = new SfWorkflowTemplate();
        template.setId(1L);
        template.setName("");
        template.setNodeConfigJson("{}");
        when(workflowTemplateMapper.selectById(1L)).thenReturn(template);

        boolean result = workflowTemplateService.validateTemplate(1L);

        assertThat(result).isFalse();
    }

    @Test
    void validateTemplate_validTemplate_returnsTrue() {
        SfWorkflowTemplate template = new SfWorkflowTemplate();
        template.setId(1L);
        template.setName("Valid Workflow");
        template.setNodeConfigJson("{\"nodes\":[]}");
        when(workflowTemplateMapper.selectById(1L)).thenReturn(template);

        boolean result = workflowTemplateService.validateTemplate(1L);

        assertThat(result).isTrue();
    }

    // ------------------------------------------------------------------
    // cloneTemplate
    // ------------------------------------------------------------------

    @Test
    void cloneTemplate_notFound_throwsWorkflowNotFound() {
        when(workflowTemplateMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> workflowTemplateService.cloneTemplate(1L, "Clone"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.WORKFLOW_NOT_FOUND.getCode());
    }

    @Test
    void cloneTemplate_nullName_throwsParamError() {
        SfWorkflowTemplate source = new SfWorkflowTemplate();
        source.setId(1L);
        when(workflowTemplateMapper.selectById(1L)).thenReturn(source);

        assertThatThrownBy(() -> workflowTemplateService.cloneTemplate(1L, null))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void cloneTemplate_blankName_throwsParamError() {
        SfWorkflowTemplate source = new SfWorkflowTemplate();
        source.setId(1L);
        when(workflowTemplateMapper.selectById(1L)).thenReturn(source);

        assertThatThrownBy(() -> workflowTemplateService.cloneTemplate(1L, "   "))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void cloneTemplate_success_createsCloneAsDraft() {
        SfWorkflowTemplate source = new SfWorkflowTemplate();
        source.setId(1L);
        source.setName("Original");
        source.setDescription("Desc");
        source.setNodeConfigJson("{}");
        when(workflowTemplateMapper.selectById(1L)).thenReturn(source);

        SfWorkflowTemplate result = workflowTemplateService.cloneTemplate(1L, "Cloned");

        assertThat(result.getName()).isEqualTo("Cloned");
        assertThat(result.getDescription()).isEqualTo("Desc");
        assertThat(result.getNodeConfigJson()).isEqualTo("{}");
        assertThat(result.getStatus()).isEqualTo("draft");
        verify(workflowTemplateMapper).insert(any(SfWorkflowTemplate.class));
    }

    @Test
    void cloneTemplate_withTenantId_setsTenantId() {
        TenantContextHolder.setTenantId("tenant-1");
        SfWorkflowTemplate source = new SfWorkflowTemplate();
        source.setId(1L);
        source.setName("Original");
        when(workflowTemplateMapper.selectById(1L)).thenReturn(source);

        SfWorkflowTemplate result = workflowTemplateService.cloneTemplate(1L, "Cloned");

        assertThat(result.getTenantId()).isEqualTo("tenant-1");
    }

    // ------------------------------------------------------------------
    // listDeployedTemplates
    // ------------------------------------------------------------------

    @Test
    void listDeployedTemplates_returnsDeployedTemplates() {
        SfWorkflowTemplate t1 = new SfWorkflowTemplate();
        t1.setStatus("deployed");
        when(workflowTemplateMapper.selectList(any())).thenReturn(List.of(t1));

        List<SfWorkflowTemplate> result = workflowTemplateService.listDeployedTemplates();

        assertThat(result).hasSize(1);
    }

    @Test
    void listDeployedTemplates_withTenantId_includesTenantFilter() {
        TenantContextHolder.setTenantId("tenant-1");
        when(workflowTemplateMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<SfWorkflowTemplate> result = workflowTemplateService.listDeployedTemplates();

        assertThat(result).isEmpty();
    }

    @Test
    void listDeployedTemplates_noDeployedTemplates_returnsEmpty() {
        when(workflowTemplateMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<SfWorkflowTemplate> result = workflowTemplateService.listDeployedTemplates();

        assertThat(result).isEmpty();
    }

    // ------------------------------------------------------------------
    // deactivateTemplate
    // ------------------------------------------------------------------

    @Test
    void deactivateTemplate_notFound_throwsWorkflowNotFound() {
        when(workflowTemplateMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> workflowTemplateService.deactivateTemplate(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.WORKFLOW_NOT_FOUND.getCode());
    }

    @Test
    void deactivateTemplate_notDeployed_throwsParamError() {
        SfWorkflowTemplate template = new SfWorkflowTemplate();
        template.setId(1L);
        template.setStatus("draft");
        when(workflowTemplateMapper.selectById(1L)).thenReturn(template);

        assertThatThrownBy(() -> workflowTemplateService.deactivateTemplate(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void deactivateTemplate_alreadyInactive_throwsParamError() {
        SfWorkflowTemplate template = new SfWorkflowTemplate();
        template.setId(1L);
        template.setStatus("inactive");
        when(workflowTemplateMapper.selectById(1L)).thenReturn(template);

        assertThatThrownBy(() -> workflowTemplateService.deactivateTemplate(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void deactivateTemplate_success_setsStatusToInactive() {
        SfWorkflowTemplate template = new SfWorkflowTemplate();
        template.setId(1L);
        template.setName("Workflow");
        template.setStatus("deployed");
        when(workflowTemplateMapper.selectById(1L)).thenReturn(template);

        SfWorkflowTemplate result = workflowTemplateService.deactivateTemplate(1L);

        assertThat(result.getStatus()).isEqualTo("inactive");
        verify(workflowTemplateMapper).updateById(template);
    }
}
