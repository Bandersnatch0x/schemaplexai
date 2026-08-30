package com.schemaplexai.task.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.schemaplexai.common.context.TenantContextHolder;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.task.dto.TaskCreateRequest;
import com.schemaplexai.task.dto.TaskStatusUpdateRequest;
import com.schemaplexai.task.dto.TaskUpdateRequest;
import com.schemaplexai.task.entity.SfTask;
import com.schemaplexai.task.mapper.SfTaskMapper;
import com.schemaplexai.task.vo.TaskPageResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Task board service (SfTaskServiceImpl) tests")
class SfTaskServiceImplTest {

    @Mock
    private SfTaskMapper taskMapper;

    private SfTaskServiceImpl taskService;

    @BeforeEach
    void setUp() {
        // Mockito does not inject into ServiceImpl's inherited generic baseMapper
        // field, so wire it explicitly to keep the chain wrappers on the mock.
        taskService = new SfTaskServiceImpl();
        ReflectionTestUtils.setField(taskService, "baseMapper", taskMapper);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("createTask applies contract defaults: BACKLOG status, P2 priority, MANUAL assignment")
    void createTask_appliesContractDefaults() {
        TenantContextHolder.setTenantId("1");
        TaskCreateRequest request = new TaskCreateRequest();
        request.setTitle(" 接入任务看板 ");
        when(taskMapper.insert(any(SfTask.class))).thenAnswer(invocation -> {
            SfTask entity = invocation.getArgument(0);
            entity.setId(11L);
            return 1;
        });
        when(taskMapper.selectById(11L)).thenAnswer(invocation -> {
            SfTask stored = new SfTask();
            stored.setId(11L);
            stored.setTenantId("1");
            stored.setTitle("接入任务看板");
            stored.setStatus("BACKLOG");
            stored.setPriority("P2");
            stored.setAssignmentType("MANUAL");
            return stored;
        });

        SfTask created = taskService.createTask(request);

        ArgumentCaptor<SfTask> captor = ArgumentCaptor.forClass(SfTask.class);
        verify(taskMapper).insert(captor.capture());
        SfTask inserted = captor.getValue();
        assertThat(inserted.getTitle()).isEqualTo("接入任务看板");
        assertThat(inserted.getStatus()).isEqualTo("BACKLOG");
        assertThat(inserted.getPriority()).isEqualTo("P2");
        assertThat(inserted.getAssignmentType()).isEqualTo("MANUAL");
        assertThat(inserted.getSkillTags()).isEmpty();
        assertThat(inserted.getTenantId()).isEqualTo("1");
        assertThat(created.getId()).isEqualTo(11L);
    }

    @Test
    @DisplayName("createTask rejects unknown priority with PARAM_ERROR")
    void createTask_rejectsUnknownPriority() {
        TenantContextHolder.setTenantId("1");
        TaskCreateRequest request = new TaskCreateRequest();
        request.setTitle("t");
        request.setPriority("P9");

        assertThatThrownBy(() -> taskService.createTask(request))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("priority")
                .hasFieldOrPropertyWithValue("code", ResultCode.PARAM_ERROR.getCode());
        verify(taskMapper, never()).insert(any(SfTask.class));
    }

    @Test
    @DisplayName("createTask fails closed without a tenant context")
    void createTask_failsClosedWithoutTenant() {
        TaskCreateRequest request = new TaskCreateRequest();
        request.setTitle("t");

        assertThatThrownBy(() -> taskService.createTask(request))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("tenant")
                .hasFieldOrPropertyWithValue("code", ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    @DisplayName("listTasks maps page into the {list,total} frontend envelope")
    void listTasks_mapsPageIntoFrontendEnvelope() {
        SfTask task = new SfTask();
        task.setId(5L);
        task.setTitle("修复网关");
        task.setStatus("IN_PROGRESS");
        Page<SfTask> page = new Page<>(1, 10);
        page.setRecords(List.of(task));
        page.setTotal(1);
        when(taskMapper.selectPage(any(Page.class), any())).thenReturn(page);

        TaskPageResult result = taskService.listTasks(1, 10, "IN_PROGRESS", null, "网关");

        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getList()).hasSize(1);
        assertThat(result.getList().get(0).getId()).isEqualTo("5");
        assertThat(result.getList().get(0).getStatus()).isEqualTo("IN_PROGRESS");
    }

    @Test
    @DisplayName("listTasks rejects an unknown status filter")
    void listTasks_rejectsUnknownStatusFilter() {
        assertThatThrownBy(() -> taskService.listTasks(1, 10, "NOPE", null, null))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("code", ResultCode.PARAM_ERROR.getCode());
        verify(taskMapper, never()).selectPage(any(), any());
    }

    @Test
    @DisplayName("updateTask applies only supplied fields")
    void updateTask_appliesOnlySuppliedFields() {
        SfTask existing = new SfTask();
        existing.setId(7L);
        existing.setTitle("old");
        existing.setPriority("P1");
        existing.setStatus("QUEUED");
        existing.setAssignmentType("AUTO");
        when(taskMapper.selectById(7L)).thenReturn(existing);
        when(taskMapper.updateById(any(SfTask.class))).thenReturn(1);

        TaskUpdateRequest request = new TaskUpdateRequest();
        request.setTitle("new");

        taskService.updateTask(7L, request);

        ArgumentCaptor<SfTask> captor = ArgumentCaptor.forClass(SfTask.class);
        verify(taskMapper).updateById(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("new");
        assertThat(captor.getValue().getPriority()).isEqualTo("P1");
        assertThat(captor.getValue().getAssignmentType()).isEqualTo("AUTO");
        assertThat(captor.getValue().getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("updateTask rejects unknown assignment type")
    void updateTask_rejectsUnknownAssignmentType() {
        SfTask existing = new SfTask();
        existing.setId(7L);
        when(taskMapper.selectById(7L)).thenReturn(existing);

        TaskUpdateRequest request = new TaskUpdateRequest();
        request.setAssignmentType("ROBOT");

        assertThatThrownBy(() -> taskService.updateTask(7L, request))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("assignmentType")
                .hasFieldOrPropertyWithValue("code", ResultCode.PARAM_ERROR.getCode());
        verify(taskMapper, never()).updateById(any(SfTask.class));
    }

    @Test
    @DisplayName("updateTask on a missing task fails with NOT_FOUND")
    void updateTask_missingTaskFailsWithNotFound() {
        when(taskMapper.selectById(404L)).thenReturn(null);

        assertThatThrownBy(() -> taskService.updateTask(404L, new TaskUpdateRequest()))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("code", ResultCode.NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("updateStatus moves the task across board columns")
    void updateStatus_movesTaskAcrossColumns() {
        SfTask existing = new SfTask();
        existing.setId(7L);
        existing.setStatus("QUEUED");
        existing.setBlockerReason(null);
        when(taskMapper.selectById(7L)).thenReturn(existing);
        when(taskMapper.updateById(any(SfTask.class))).thenReturn(1);

        TaskStatusUpdateRequest request = new TaskStatusUpdateRequest();
        request.setStatus("IN_PROGRESS");
        taskService.updateStatus(7L, request);

        ArgumentCaptor<SfTask> captor = ArgumentCaptor.forClass(SfTask.class);
        verify(taskMapper).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("IN_PROGRESS");
        assertThat(captor.getValue().getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("updateStatus into BLOCKED requires a blocker reason and stores it")
    void updateStatus_blockedRequiresReason() {
        SfTask existing = new SfTask();
        existing.setId(7L);
        existing.setStatus("IN_PROGRESS");
        when(taskMapper.selectById(7L)).thenReturn(existing);
        when(taskMapper.updateById(any(SfTask.class))).thenReturn(1);

        TaskStatusUpdateRequest withoutReason = new TaskStatusUpdateRequest();
        withoutReason.setStatus("BLOCKED");
        assertThatThrownBy(() -> taskService.updateStatus(7L, withoutReason))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("blockerReason")
                .hasFieldOrPropertyWithValue("code", ResultCode.PARAM_ERROR.getCode());

        TaskStatusUpdateRequest withReason = new TaskStatusUpdateRequest();
        withReason.setStatus("BLOCKED");
        withReason.setBlockerReason(" waiting on API key ");
        taskService.updateStatus(7L, withReason);

        ArgumentCaptor<SfTask> captor = ArgumentCaptor.forClass(SfTask.class);
        verify(taskMapper).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("BLOCKED");
        assertThat(captor.getValue().getBlockerReason()).isEqualTo("waiting on API key");
    }

    @Test
    @DisplayName("updateStatus leaving BLOCKED clears the stale blocker reason")
    void updateStatus_leavingBlockedClearsReason() {
        SfTask existing = new SfTask();
        existing.setId(7L);
        existing.setStatus("BLOCKED");
        existing.setBlockerReason("old blocker");
        when(taskMapper.selectById(7L)).thenReturn(existing);
        when(taskMapper.updateById(any(SfTask.class))).thenReturn(1);

        TaskStatusUpdateRequest request = new TaskStatusUpdateRequest();
        request.setStatus("IN_PROGRESS");
        taskService.updateStatus(7L, request);

        ArgumentCaptor<SfTask> captor = ArgumentCaptor.forClass(SfTask.class);
        verify(taskMapper).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("IN_PROGRESS");
        assertThat(captor.getValue().getBlockerReason()).isNull();
    }

    @Test
    @DisplayName("updateStatus rejects unknown statuses")
    void updateStatus_rejectsUnknownStatus() {
        SfTask existing = new SfTask();
        existing.setId(7L);
        when(taskMapper.selectById(7L)).thenReturn(existing);

        TaskStatusUpdateRequest request = new TaskStatusUpdateRequest();
        request.setStatus("ARCHIVED");

        assertThatThrownBy(() -> taskService.updateStatus(7L, request))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("code", ResultCode.PARAM_ERROR.getCode());
        verify(taskMapper, never()).updateById(any(SfTask.class));
    }

    @Test
    @DisplayName("deleteTask guards existence then soft-deletes")
    void deleteTask_guardsExistenceThenDeletes() {
        SfTask existing = new SfTask();
        existing.setId(7L);
        when(taskMapper.selectById(7L)).thenReturn(existing);
        // removeById on a @TableLogic entity resolves to the entity-overload deleteById
        when(taskMapper.deleteById(any(SfTask.class))).thenReturn(1);

        taskService.deleteTask(7L);

        ArgumentCaptor<SfTask> deleteCaptor = ArgumentCaptor.forClass(SfTask.class);
        verify(taskMapper).deleteById(deleteCaptor.capture());
        assertThat(deleteCaptor.getValue().getId()).isEqualTo(7L);

        when(taskMapper.selectById(anyLong())).thenReturn(null);
        assertThatThrownBy(() -> taskService.deleteTask(404L))
                .isInstanceOf(BaseException.class)
                .hasFieldOrPropertyWithValue("code", ResultCode.NOT_FOUND.getCode());
    }
}
