package com.schemaplexai.context.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.schemaplexai.common.context.TenantContextHolder;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.context.entity.SfWorkspace;
import com.schemaplexai.context.mapper.SfWorkspaceMapper;
import com.schemaplexai.context.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Transactional(rollbackFor = Exception.class)
@Service
@RequiredArgsConstructor
public class WorkspaceServiceImpl extends ServiceImpl<SfWorkspaceMapper, SfWorkspace> implements WorkspaceService {

    private static final String DEFAULT_WORKSPACE_NAME = "Default Workspace";
    private static final String DEFAULT_WORKSPACE_DESCRIPTION = "Auto-created default workspace";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SfWorkspace createDefaultWorkspace(String tenantId) {
        SfWorkspace workspace = new SfWorkspace();
        workspace.setTenantId(tenantId);
        workspace.setName(DEFAULT_WORKSPACE_NAME);
        workspace.setDescription(DEFAULT_WORKSPACE_DESCRIPTION);
        workspace.setParentId(0L);
        baseMapper.insert(workspace);
        log.info("Created default workspace for tenant: {}, workspaceId={}", tenantId, workspace.getId());
        return workspace;
    }

    @Override
    public void validateWorkspaceAccess(Long workspaceId) {
        SfWorkspace workspace = baseMapper.selectById(workspaceId);
        if (workspace == null) {
            throw new BaseException(ResultCode.NOT_FOUND, "Workspace not found: " + workspaceId);
        }
        String currentTenantId = TenantContextHolder.getTenantId();
        if (currentTenantId != null && !currentTenantId.equals(workspace.getTenantId())) {
            throw new BaseException(ResultCode.FORBIDDEN, "Access denied to workspace: " + workspaceId);
        }
        log.debug("Workspace access validated: id={}", workspaceId);
    }

    @Override
    public List<SfWorkspace> listWorkspacesByTenant(String tenantId) {
        return baseMapper.selectList(
                new LambdaQueryWrapper<SfWorkspace>()
                        .eq(SfWorkspace::getTenantId, tenantId)
                        .orderByDesc(SfWorkspace::getCreatedAt));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void archiveWorkspace(Long workspaceId) {
        SfWorkspace workspace = baseMapper.selectById(workspaceId);
        if (workspace == null) {
            throw new BaseException(ResultCode.NOT_FOUND, "Workspace not found: " + workspaceId);
        }
        baseMapper.deleteById(workspaceId);
        log.info("Archived workspace: id={}", workspaceId);
    }
}
