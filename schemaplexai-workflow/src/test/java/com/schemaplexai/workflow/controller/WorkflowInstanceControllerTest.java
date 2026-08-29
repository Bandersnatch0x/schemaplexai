package com.schemaplexai.workflow.controller;

import com.schemaplexai.common.page.PageParam;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.workflow.entity.SfWorkflowInstance;
import com.schemaplexai.workflow.service.WorkflowInstanceService;
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

@WebMvcTest(WorkflowInstanceController.class)
class WorkflowInstanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WorkflowInstanceService workflowInstanceService;

    @Test
    void create_returnsId() throws Exception {
        when(workflowInstanceService.save(any())).thenReturn(true);

        mockMvc.perform(post("/workflow/instances")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"templateId\":1,\"status\":\"PENDING\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void update_returnsBoolean() throws Exception {
        when(workflowInstanceService.updateById(any())).thenReturn(true);

        mockMvc.perform(put("/workflow/instances/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"templateId\":1,\"status\":\"RUNNING\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void delete_returnsBoolean() throws Exception {
        when(workflowInstanceService.removeById(1L)).thenReturn(true);

        mockMvc.perform(delete("/workflow/instances/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void get_found() throws Exception {
        SfWorkflowInstance instance = new SfWorkflowInstance();
        instance.setId(1L);
        instance.setStatus("PENDING");
        when(workflowInstanceService.getById(1L)).thenReturn(instance);

        mockMvc.perform(get("/workflow/instances/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void get_notFound() throws Exception {
        when(workflowInstanceService.getById(1L)).thenReturn(null);

        mockMvc.perform(get("/workflow/instances/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.NOT_FOUND.getCode()));
    }

    @Test
    void page_returnsList() throws Exception {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<SfWorkflowInstance> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>();
        page.setRecords(List.of());
        page.setTotal(0L);
        when(workflowInstanceService.page(any())).thenReturn(page);

        mockMvc.perform(get("/workflow/instances/page")
                        .param("current", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void trigger_returnsSuccess() throws Exception {
        doNothing().when(workflowInstanceService).trigger(1L);

        mockMvc.perform(post("/workflow/instances/1/trigger"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void cancel_returnsSuccess() throws Exception {
        doNothing().when(workflowInstanceService).cancel(1L);

        mockMvc.perform(post("/workflow/instances/1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(true));

        verify(workflowInstanceService).cancel(1L);
    }

    @Test
    void approve_passesComment() throws Exception {
        doNothing().when(workflowInstanceService).approve(1L, "ok");

        mockMvc.perform(post("/workflow/instances/1/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"ok\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(true));

        verify(workflowInstanceService).approve(1L, "ok");
    }

    @Test
    void approve_withoutBody_passesNullComment() throws Exception {
        doNothing().when(workflowInstanceService).approve(eq(1L), isNull());

        mockMvc.perform(post("/workflow/instances/1/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(true));

        verify(workflowInstanceService).approve(1L, null);
    }

    @Test
    void reject_passesReason() throws Exception {
        doNothing().when(workflowInstanceService).reject(1L, "too risky");

        mockMvc.perform(post("/workflow/instances/1/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"too risky\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(true));

        verify(workflowInstanceService).reject(1L, "too risky");
    }
}
