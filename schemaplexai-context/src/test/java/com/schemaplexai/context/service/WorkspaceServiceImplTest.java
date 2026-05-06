package com.schemaplexai.context.service;

import com.schemaplexai.common.context.TenantContextHolder;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.context.entity.SfWorkspace;
import com.schemaplexai.context.mapper.SfWorkspaceMapper;
import com.schemaplexai.context.service.impl.WorkspaceServiceImpl;
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
class WorkspaceServiceImplTest {

    @Mock
    private SfWorkspaceMapper workspaceMapper;

    @InjectMocks
    private WorkspaceServiceImpl workspaceService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(workspaceService, "baseMapper", workspaceMapper);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    // ------------------------------------------------------------------
    // createDefaultWorkspace
    // ------------------------------------------------------------------

    @Test
    void createDefaultWorkspace_success_createsWorkspaceWithDefaults() {
        SfWorkspace result = workspaceService.createDefaultWorkspace("tenant-1");

        assertThat(result.getTenantId()).isEqualTo("tenant-1");
        assertThat(result.getName()).isEqualTo("Default Workspace");
        assertThat(result.getDescription()).isEqualTo("Auto-created default workspace");
        assertThat(result.getParentId()).isEqualTo(0L);
        verify(workspaceMapper).insert(any(SfWorkspace.class));
    }

    @Test
    void createDefaultWorkspace_nullTenantId_createsWorkspace() {
        SfWorkspace result = workspaceService.createDefaultWorkspace(null);

        assertThat(result.getTenantId()).isNull();
        assertThat(result.getName()).isEqualTo("Default Workspace");
        verify(workspaceMapper).insert(any(SfWorkspace.class));
    }

    // ------------------------------------------------------------------
    // validateWorkspaceAccess
    // ------------------------------------------------------------------

    @Test
    void validateWorkspaceAccess_workspaceNotFound_throwsNotFound() {
        when(workspaceMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> workspaceService.validateWorkspaceAccess(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void validateWorkspaceAccess_tenantMismatch_throwsForbidden() {
        TenantContextHolder.setTenantId("tenant-2");
        SfWorkspace workspace = new SfWorkspace();
        workspace.setId(1L);
        workspace.setTenantId("tenant-1");
        when(workspaceMapper.selectById(1L)).thenReturn(workspace);

        assertThatThrownBy(() -> workspaceService.validateWorkspaceAccess(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.FORBIDDEN.getCode());
    }

    @Test
    void validateWorkspaceAccess_sameTenant_succeeds() {
        TenantContextHolder.setTenantId("tenant-1");
        SfWorkspace workspace = new SfWorkspace();
        workspace.setId(1L);
        workspace.setTenantId("tenant-1");
        when(workspaceMapper.selectById(1L)).thenReturn(workspace);

        workspaceService.validateWorkspaceAccess(1L);

        // no exception thrown
    }

    @Test
    void validateWorkspaceAccess_nullCurrentTenant_succeeds() {
        SfWorkspace workspace = new SfWorkspace();
        workspace.setId(1L);
        workspace.setTenantId("tenant-1");
        when(workspaceMapper.selectById(1L)).thenReturn(workspace);

        workspaceService.validateWorkspaceAccess(1L);

        // no exception thrown
    }

    @Test
    void validateWorkspaceAccess_nullWorkspaceTenant_throwsForbidden() {
        TenantContextHolder.setTenantId("tenant-1");
        SfWorkspace workspace = new SfWorkspace();
        workspace.setId(1L);
        workspace.setTenantId(null);
        when(workspaceMapper.selectById(1L)).thenReturn(workspace);

        assertThatThrownBy(() -> workspaceService.validateWorkspaceAccess(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.FORBIDDEN.getCode());
    }

    // ------------------------------------------------------------------
    // listWorkspacesByTenant
    // ------------------------------------------------------------------

    @Test
    void listWorkspacesByTenant_returnsWorkspacesOrderedByCreatedAt() {
        SfWorkspace w1 = new SfWorkspace();
        w1.setName("W1");
        SfWorkspace w2 = new SfWorkspace();
        w2.setName("W2");
        when(workspaceMapper.selectList(any())).thenReturn(List.of(w1, w2));

        List<SfWorkspace> result = workspaceService.listWorkspacesByTenant("tenant-1");

        assertThat(result).hasSize(2);
    }

    @Test
    void listWorkspacesByTenant_noWorkspaces_returnsEmpty() {
        when(workspaceMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<SfWorkspace> result = workspaceService.listWorkspacesByTenant("tenant-1");

        assertThat(result).isEmpty();
    }

    // ------------------------------------------------------------------
    // archiveWorkspace
    // ------------------------------------------------------------------

    @Test
    void archiveWorkspace_notFound_throwsNotFound() {
        when(workspaceMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> workspaceService.archiveWorkspace(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void archiveWorkspace_success_deletesWorkspace() {
        SfWorkspace workspace = new SfWorkspace();
        workspace.setId(1L);
        when(workspaceMapper.selectById(1L)).thenReturn(workspace);

        workspaceService.archiveWorkspace(1L);

        verify(workspaceMapper).deleteById(1L);
    }
}
