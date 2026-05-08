package com.schemaplexai.workflow.controller;

import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.workflow.service.WorkflowDeployService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WorkflowBpmnController.class)
class WorkflowBpmnControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WorkflowDeployService workflowDeployService;

    @Test
    void listDeployedProcesses_returnsList() throws Exception {
        WorkflowDeployService.ProcessDefinitionInfo info =
                new WorkflowDeployService.ProcessDefinitionInfo("id1", "key1", "Name", 1, "dep1", false);
        when(workflowDeployService.listDeployedProcesses()).thenReturn(List.of(info));

        mockMvc.perform(get("/workflow/bpmn/processes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].key").value("key1"));
    }

    @Test
    void startProcess_success() throws Exception {
        when(workflowDeployService.startProcessInstance(eq("key1"), eq("biz"), any()))
                .thenReturn("inst-1");

        mockMvc.perform(post("/workflow/bpmn/processes/key1/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"businessKey\":\"biz\",\"variables\":{}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("inst-1"));
    }

    @Test
    void startProcess_failure_returnsError() throws Exception {
        when(workflowDeployService.startProcessInstance(eq("key1"), eq("biz"), any()))
                .thenThrow(new RuntimeException("not found"));

        mockMvc.perform(post("/workflow/bpmn/processes/key1/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"businessKey\":\"biz\",\"variables\":{}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ResultCode.WORKFLOW_NOT_FOUND.getCode()));
    }

    @Test
    void suspendProcessDefinition_returnsSuccess() throws Exception {
        doNothing().when(workflowDeployService).suspendProcessDefinition("key1");

        mockMvc.perform(post("/workflow/bpmn/processes/key1/suspend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void activateProcessDefinition_returnsSuccess() throws Exception {
        doNothing().when(workflowDeployService).activateProcessDefinition("key1");

        mockMvc.perform(post("/workflow/bpmn/processes/key1/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(true));
    }
}
