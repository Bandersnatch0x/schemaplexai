package com.schemaplexai.workflow.controller;

import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.workflow.entity.SfWorkflowTemplate;
import com.schemaplexai.workflow.service.WorkflowTemplateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WorkflowTemplateController.class)
class WorkflowTemplateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WorkflowTemplateService workflowTemplateService;

    @Test
    void create_returnsId() throws Exception {
        when(workflowTemplateService.save(any())).thenReturn(true);

        mockMvc.perform(post("/workflow/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test\",\"status\":\"draft\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void update_returnsBoolean() throws Exception {
        when(workflowTemplateService.updateById(any())).thenReturn(true);

        mockMvc.perform(put("/workflow/templates/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void delete_returnsBoolean() throws Exception {
        when(workflowTemplateService.removeById(1L)).thenReturn(true);

        mockMvc.perform(delete("/workflow/templates/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void get_found() throws Exception {
        SfWorkflowTemplate template = new SfWorkflowTemplate();
        template.setId(1L);
        template.setName("Test Workflow");
        when(workflowTemplateService.getById(1L)).thenReturn(template);

        mockMvc.perform(get("/workflow/templates/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("Test Workflow"));
    }

    @Test
    void get_notFound() throws Exception {
        when(workflowTemplateService.getById(1L)).thenReturn(null);

        mockMvc.perform(get("/workflow/templates/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.NOT_FOUND.getCode()));
    }

    @Test
    void page_returnsList() throws Exception {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<SfWorkflowTemplate> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>();
        page.setRecords(List.of());
        page.setTotal(0L);
        when(workflowTemplateService.page(any())).thenReturn(page);

        mockMvc.perform(get("/workflow/templates/page")
                        .param("current", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void deployTemplate_returnsTemplate() throws Exception {
        SfWorkflowTemplate template = new SfWorkflowTemplate();
        template.setId(1L);
        template.setStatus("deployed");
        when(workflowTemplateService.deployTemplate(1L)).thenReturn(template);

        mockMvc.perform(post("/workflow/templates/1/deploy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("deployed"));
    }

    @Test
    void validateTemplate_returnsBoolean() throws Exception {
        when(workflowTemplateService.validateTemplate(1L)).thenReturn(true);

        mockMvc.perform(post("/workflow/templates/1/validate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void cloneTemplate_returnsCloned() throws Exception {
        SfWorkflowTemplate clone = new SfWorkflowTemplate();
        clone.setId(2L);
        clone.setName("Cloned");
        when(workflowTemplateService.cloneTemplate(1L, "Cloned")).thenReturn(clone);

        mockMvc.perform(post("/workflow/templates/1/clone")
                        .param("newName", "Cloned"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("Cloned"));
    }

    @Test
    void listDeployedTemplates_returnsList() throws Exception {
        SfWorkflowTemplate t1 = new SfWorkflowTemplate();
        t1.setId(1L);
        t1.setStatus("deployed");
        when(workflowTemplateService.listDeployedTemplates()).thenReturn(List.of(t1));

        mockMvc.perform(get("/workflow/templates/deployed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void deactivateTemplate_returnsTemplate() throws Exception {
        SfWorkflowTemplate template = new SfWorkflowTemplate();
        template.setId(1L);
        template.setStatus("inactive");
        when(workflowTemplateService.deactivateTemplate(1L)).thenReturn(template);

        mockMvc.perform(post("/workflow/templates/1/deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("inactive"));
    }
}
