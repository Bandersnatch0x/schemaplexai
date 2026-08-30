package com.schemaplexai.task.controller;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.exception.GlobalExceptionHandler;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.task.entity.SfTask;
import com.schemaplexai.task.service.SfTaskCommentService;
import com.schemaplexai.task.service.SfTaskService;
import com.schemaplexai.task.vo.TaskCommentVO;
import com.schemaplexai.task.vo.TaskPageResult;
import com.schemaplexai.task.web.TaskUserContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(excludeAutoConfiguration = SecurityAutoConfiguration.class)
@ContextConfiguration(classes = {TaskController.class, GlobalExceptionHandler.class})
@DisplayName("Task board controller MockMvc tests (frontend contract)")
class TaskControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SfTaskService taskService;

    @MockBean
    private SfTaskCommentService taskCommentService;

    @BeforeEach
    void setUserContext() {
        TaskUserContextHolder.setUserId(7L);
    }

    @AfterEach
    void clearUserContext() {
        TaskUserContextHolder.clear();
    }

    @Test
    @DisplayName("GET /task/tasks returns the {list,total} envelope with string ids")
    void listTasks_returnsFrontendEnvelope() throws Exception {
        var vo = new com.schemaplexai.task.vo.TaskVO();
        vo.setId("11");
        vo.setTenantId("1");
        vo.setTitle("接入任务看板");
        vo.setPriority("P1");
        vo.setStatus("BACKLOG");
        vo.setAssignmentType("MANUAL");
        vo.setSkillTags(List.of("backend", "mq"));
        vo.setCreatedAt(LocalDateTime.of(2026, 8, 30, 10, 0));
        when(taskService.listTasks(1, 20, "BACKLOG", null, null))
                .thenReturn(new TaskPageResult(List.of(vo), 1));

        mockMvc.perform(get("/task/tasks")
                        .param("page", "1")
                        .param("pageSize", "20")
                        .param("status", "BACKLOG")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].id").value("11"))
                .andExpect(jsonPath("$.data.list[0].skillTags[1]").value("mq"))
                .andExpect(jsonPath("$.data.list[0].createdAt").isNotEmpty());
    }

    @Test
    @DisplayName("GET /task/tasks surfaces service rejections through the Result envelope")
    void listTasks_surfacesServiceRejections() throws Exception {
        when(taskService.listTasks(1, 10, "NOPE", null, null))
                .thenThrow(new BaseException(ResultCode.PARAM_ERROR, "unknown task status: NOPE"));

        mockMvc.perform(get("/task/tasks").param("status", "NOPE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("unknown task status: NOPE"));
    }

    @Test
    @DisplayName("GET /task/tasks/{id} returns the task and 404-code for unknown tasks")
    void getTask_foundAndNotFound() throws Exception {
        SfTask task = new SfTask();
        task.setId(11L);
        task.setTitle("t");
        when(taskService.getById(11L)).thenReturn(task);

        mockMvc.perform(get("/task/tasks/11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value("11"));

        when(taskService.getById(404L)).thenReturn(null);
        mockMvc.perform(get("/task/tasks/404"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("non-numeric ids are rejected with a param error")
    void invalidIdsAreRejected() throws Exception {
        mockMvc.perform(get("/task/tasks/abc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("POST /task/tasks creates a task and returns the stored view")
    void createTask_returnsStoredView() throws Exception {
        SfTask created = new SfTask();
        created.setId(12L);
        created.setTitle("new task");
        created.setStatus("BACKLOG");
        when(taskService.createTask(any())).thenReturn(created);

        mockMvc.perform(post("/task/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"new task\",\"priority\":\"P0\",\"skillTags\":[\"rag\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value("12"))
                .andExpect(jsonPath("$.data.status").value("BACKLOG"));

        ArgumentCaptor<com.schemaplexai.task.dto.TaskCreateRequest> captor =
                ArgumentCaptor.forClass(com.schemaplexai.task.dto.TaskCreateRequest.class);
        verify(taskService).createTask(captor.capture());
        assertThat(captor.getValue().getPriority()).isEqualTo("P0");
        assertThat(captor.getValue().getSkillTags()).containsExactly("rag");
    }

    @Test
    @DisplayName("POST /task/tasks rejects a blank title with a validation error")
    void createTask_rejectsBlankTitle() throws Exception {
        mockMvc.perform(post("/task/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("PUT /task/tasks/{id} applies a partial update")
    void updateTask_appliesPartialUpdate() throws Exception {
        SfTask updated = new SfTask();
        updated.setId(11L);
        updated.setTitle("renamed");
        when(taskService.updateTask(eq(11L), any())).thenReturn(updated);

        mockMvc.perform(put("/task/tasks/11")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"renamed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.title").value("renamed"));
    }

    @Test
    @DisplayName("DELETE /task/tasks/{id} soft-deletes via the service")
    void deleteTask_delegatesToService() throws Exception {
        mockMvc.perform(delete("/task/tasks/11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(taskService).deleteTask(11L);
    }

    @Test
    @DisplayName("PUT /task/tasks/{id}/status forwards status and blocker reason")
    void updateStatus_forwardsPayload() throws Exception {
        mockMvc.perform(put("/task/tasks/11/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"BLOCKED\",\"blockerReason\":\"waiting on review\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<com.schemaplexai.task.dto.TaskStatusUpdateRequest> captor =
                ArgumentCaptor.forClass(com.schemaplexai.task.dto.TaskStatusUpdateRequest.class);
        verify(taskService).updateStatus(eq(11L), captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("BLOCKED");
        assertThat(captor.getValue().getBlockerReason()).isEqualTo("waiting on review");
    }

    @Test
    @DisplayName("PUT /task/tasks/{id}/status requires the status field")
    void updateStatus_requiresStatus() throws Exception {
        mockMvc.perform(put("/task/tasks/11/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"blockerReason\":\"x\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("GET /task/tasks/{id}/comments returns the comment list")
    void listComments_returnsList() throws Exception {
        var comment = new TaskCommentVO();
        comment.setId("1");
        comment.setTaskId("11");
        comment.setContent("lgtm");
        comment.setAuthorId("7");
        comment.setAuthorName("alice");
        when(taskCommentService.listComments(11L)).thenReturn(List.of(comment));

        mockMvc.perform(get("/task/tasks/11/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].content").value("lgtm"))
                .andExpect(jsonPath("$.data[0].authorName").value("alice"));
    }

    @Test
    @DisplayName("POST /task/tasks/{id}/comments attributes the comment to the gateway user")
    void addComment_usesGatewayUserIdentity() throws Exception {
        var created = new TaskCommentVO();
        created.setId("2");
        created.setTaskId("11");
        created.setContent("note");
        created.setAuthorId("7");
        when(taskCommentService.addComment(eq(11L), eq("note"), eq(7L))).thenReturn(created);

        mockMvc.perform(post("/task/tasks/11/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"note\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.authorId").value("7"));

        verify(taskCommentService).addComment(11L, "note", 7L);
    }

    @Test
    @DisplayName("POST /task/tasks/{id}/comments passes a null author when no user context exists")
    void addComment_nullAuthorWithoutUserContext() throws Exception {
        TaskUserContextHolder.clear();
        when(taskCommentService.addComment(eq(11L), eq("note"), isNull()))
                .thenThrow(new BaseException(ResultCode.UNAUTHORIZED, "missing user identity for comment author"));

        mockMvc.perform(post("/task/tasks/11/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"note\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }
}
