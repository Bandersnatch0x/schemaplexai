package com.schemaplexai.web.controller;

import com.schemaplexai.common.exception.GlobalExceptionHandler;
import com.schemaplexai.web.service.execution.EngineExecutionQueryPort;
import com.schemaplexai.web.service.execution.ExecutionLifecyclePort;
import com.schemaplexai.web.service.execution.ExecutionStatusPort;
import com.schemaplexai.web.vo.ExecutionStatusVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(excludeAutoConfiguration = SecurityAutoConfiguration.class)
@ContextConfiguration(classes = {ExecutionWebController.class, GlobalExceptionHandler.class})
@DisplayName("M6.4: Execution Web Controller MockMvc Integration Tests")
class ExecutionWebControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExecutionLifecyclePort lifecyclePort;

    @MockBean
    private ExecutionStatusPort statusPort;

    @MockBean
    private EngineExecutionQueryPort queryPort;

    @Test
    @DisplayName("GET /web/executions/42 returns typed ExecutionStatusVO")
    void getExecutionStatus_returnsTypedJson() throws Exception {
        ExecutionStatusVO status = new ExecutionStatusVO();
        status.setExecutionId(42L);
        status.setAgentId(7L);
        status.setState("RUNNING");
        LocalDateTime createdAt = LocalDateTime.of(2026, 6, 1, 10, 0, 0);
        status.setCreatedAt(createdAt);
        when(statusPort.getExecutionStatus(42L)).thenReturn(status);

        mockMvc.perform(get("/web/executions/42")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.executionId").value(42))
                .andExpect(jsonPath("$.data.agentId").value(7))
                .andExpect(jsonPath("$.data.state").value("RUNNING"))
                .andExpect(jsonPath("$.data.createdAt").isNotEmpty());
    }

    @Test
    @DisplayName("POST /web/executions/42/pause returns success")
    void pauseExecution_returnsSuccess() throws Exception {
        mockMvc.perform(post("/web/executions/42/pause")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(lifecyclePort).pauseExecution(42L);
    }

    @Test
    @DisplayName("POST /web/executions/42/resume returns success")
    void resumeExecution_returnsSuccess() throws Exception {
        mockMvc.perform(post("/web/executions/42/resume")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(lifecyclePort).resumeExecution(42L);
    }

    @Test
    @DisplayName("POST /web/executions/42/cancel returns success")
    void cancelExecution_returnsSuccess() throws Exception {
        mockMvc.perform(post("/web/executions/42/cancel")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(lifecyclePort).cancelExecution(42L);
    }

    @Test
    @DisplayName("GET /web/executions with pagination params returns page")
    void listExecutions_withPagination_returnsOk() throws Exception {
        mockMvc.perform(get("/web/executions")
                        .param("page", "1")
                        .param("size", "20")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(queryPort).listExecutions(1, 20, null, null);
    }

    @Test
    @DisplayName("GET /web/executions with state filter delegates correctly")
    void listExecutions_withStateFilter() throws Exception {
        mockMvc.perform(get("/web/executions")
                        .param("page", "1")
                        .param("size", "10")
                        .param("state", "RUNNING")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(queryPort).listExecutions(1, 10, "RUNNING", null);
    }
}
