package com.schemaplexai.task.controller;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.exception.GlobalExceptionHandler;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.task.service.JobService;
import com.schemaplexai.task.vo.JobPageResult;
import com.schemaplexai.task.vo.JobRecordVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(excludeAutoConfiguration = SecurityAutoConfiguration.class)
@ContextConfiguration(classes = {JobController.class, GlobalExceptionHandler.class})
@DisplayName("Job board controller MockMvc tests (frontend contract)")
class JobControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JobService jobService;

    @Test
    @DisplayName("GET /task/jobs returns the {list,total} JobRecord envelope")
    void listJobs_returnsFrontendEnvelope() throws Exception {
        var job = new JobRecordVO();
        job.setId("21");
        job.setName("8b2f6c1e-4f4b-4b41-9a55-2f8f4a3d2f11");
        job.setQueue("sf.cost");
        job.setStatus("PENDING");
        job.setRetryCount(2);
        job.setMaxRetries(3);
        job.setCreatedAt(LocalDateTime.of(2026, 8, 29, 12, 0));
        job.setUpdatedAt(LocalDateTime.of(2026, 8, 29, 12, 5));
        when(jobService.listJobs(2, 5, "sf.cost")).thenReturn(new JobPageResult(List.of(job), 1));

        mockMvc.perform(get("/task/jobs")
                        .param("page", "2")
                        .param("pageSize", "5")
                        .param("queue", "sf.cost")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].id").value("21"))
                .andExpect(jsonPath("$.data.list[0].retryCount").value(2))
                .andExpect(jsonPath("$.data.list[0].maxRetries").value(3));
    }

    @Test
    @DisplayName("POST /task/jobs/{id}/retry delegates to the job service")
    void retryJob_delegatesToService() throws Exception {
        mockMvc.perform(post("/task/jobs/21/retry"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(jobService).retryJob(21L);
    }

    @Test
    @DisplayName("POST /task/jobs/{id}/cancel delegates to the job service")
    void cancelJob_delegatesToService() throws Exception {
        mockMvc.perform(post("/task/jobs/21/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(jobService).cancelJob(21L);
    }

    @Test
    @DisplayName("job actions surface service rejections through the Result envelope")
    void jobActions_surfaceServiceRejections() throws Exception {
        doThrow(new BaseException(ResultCode.NOT_FOUND, "job not found: 404"))
                .when(jobService).retryJob(404L);

        mockMvc.perform(post("/task/jobs/404/retry"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));

        mockMvc.perform(post("/task/jobs/abc/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }
}
