package com.schemaplexai.context.controller;

import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.context.entity.SfWorkspace;
import com.schemaplexai.context.service.WorkspaceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkspaceControllerTest {

    @Mock
    private WorkspaceService workspaceService;

    @InjectMocks
    private WorkspaceController controller;

    @Test
    void create_success() {
        SfWorkspace workspace = new SfWorkspace();
        workspace.setId(1L);
        when(workspaceService.save(workspace)).thenReturn(true);
        Result<Long> result = controller.create(workspace);
        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEqualTo(1L);
    }

    @Test
    void update_success() {
        SfWorkspace workspace = new SfWorkspace();
        when(workspaceService.updateById(workspace)).thenReturn(true);
        Result<Boolean> result = controller.update(1L, workspace);
        assertThat(result.getData()).isTrue();
        assertThat(workspace.getId()).isEqualTo(1L);
    }

    @Test
    void delete_success() {
        when(workspaceService.removeById(1L)).thenReturn(true);
        Result<Boolean> result = controller.delete(1L);
        assertThat(result.getData()).isTrue();
    }

    @Test
    void get_found() {
        SfWorkspace workspace = new SfWorkspace();
        when(workspaceService.getById(1L)).thenReturn(workspace);
        Result<SfWorkspace> result = controller.get(1L);
        assertThat(result.getCode()).isEqualTo(200);
    }

    @Test
    void get_notFound() {
        when(workspaceService.getById(1L)).thenReturn(null);
        Result<SfWorkspace> result = controller.get(1L);
        assertThat(result.getCode()).isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void createDefaultWorkspace_success() {
        SfWorkspace workspace = new SfWorkspace();
        when(workspaceService.createDefaultWorkspace("t1")).thenReturn(workspace);
        Result<SfWorkspace> result = controller.createDefaultWorkspace("t1");
        assertThat(result.getCode()).isEqualTo(200);
    }

    @Test
    void validateWorkspaceAccess_success() {
        doNothing().when(workspaceService).validateWorkspaceAccess(1L);
        Result<Void> result = controller.validateWorkspaceAccess(1L);
        assertThat(result.getCode()).isEqualTo(200);
    }

    @Test
    void listWorkspacesByTenant_success() {
        when(workspaceService.listWorkspacesByTenant("t1")).thenReturn(List.of());
        Result<List<SfWorkspace>> result = controller.listWorkspacesByTenant("t1");
        assertThat(result.getCode()).isEqualTo(200);
    }

    @Test
    void archiveWorkspace_success() {
        doNothing().when(workspaceService).archiveWorkspace(1L);
        Result<Void> result = controller.archiveWorkspace(1L);
        assertThat(result.getCode()).isEqualTo(200);
    }
}
