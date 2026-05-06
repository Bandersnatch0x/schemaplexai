package com.schemaplexai.context.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.schemaplexai.context.entity.SfWorkspace;

import java.util.List;

public interface WorkspaceService extends IService<SfWorkspace> {

    /**
     * Create a default workspace for a tenant.
     */
    SfWorkspace createDefaultWorkspace(String tenantId);

    /**
     * Validate that the current tenant has access to the workspace.
     */
    void validateWorkspaceAccess(Long workspaceId);

    /**
     * List workspaces filtered by tenant.
     */
    List<SfWorkspace> listWorkspacesByTenant(String tenantId);

    /**
     * Archive (soft-delete) a workspace.
     */
    void archiveWorkspace(Long workspaceId);
}
