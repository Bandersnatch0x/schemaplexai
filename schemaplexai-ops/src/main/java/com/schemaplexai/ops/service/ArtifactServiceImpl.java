package com.schemaplexai.ops.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.schemaplexai.common.context.TenantContextHolder;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.ops.entity.SfArtifact;
import com.schemaplexai.ops.mapper.ArtifactMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Transactional(rollbackFor = Exception.class)
@Service
@RequiredArgsConstructor
public class ArtifactServiceImpl extends ServiceImpl<ArtifactMapper, SfArtifact> implements ArtifactService {

    private static final int STATUS_ACTIVE = 0;
    private static final int STATUS_ARCHIVED = 1;

    @Override
    public SfArtifact uploadArtifact(SfArtifact artifact) {
        if (artifact.getName() == null || artifact.getName().isBlank()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Artifact name is required");
        }
        if (artifact.getArtifactType() == null || artifact.getArtifactType().isBlank()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Artifact type is required");
        }
        if (artifact.getFileUrl() == null || artifact.getFileUrl().isBlank()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Artifact file URL is required");
        }
        artifact.setStatus(STATUS_ACTIVE);
        baseMapper.insert(artifact);
        log.info("Uploaded artifact: id={}, name={}, type={}", artifact.getId(), artifact.getName(), artifact.getArtifactType());
        return artifact;
    }

    @Override
    public SfArtifact downloadArtifact(Long artifactId) {
        SfArtifact artifact = baseMapper.selectById(artifactId);
        if (artifact == null) {
            throw new BaseException(ResultCode.NOT_FOUND, "Artifact not found: " + artifactId);
        }
        if (artifact.getStatus() != null && artifact.getStatus() == STATUS_ARCHIVED) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Artifact is archived and cannot be downloaded: " + artifactId);
        }
        log.info("Downloaded artifact: id={}, name={}", artifactId, artifact.getName());
        return artifact;
    }

    @Override
    public boolean validateArtifact(Long artifactId) {
        SfArtifact artifact = baseMapper.selectById(artifactId);
        if (artifact == null) {
            throw new BaseException(ResultCode.NOT_FOUND, "Artifact not found: " + artifactId);
        }
        if (artifact.getFileUrl() == null || artifact.getFileUrl().isBlank()) {
            log.warn("Artifact validation failed - missing file URL: id={}", artifactId);
            return false;
        }
        if (artifact.getVersion() == null || artifact.getVersion().isBlank()) {
            log.warn("Artifact validation failed - missing version: id={}", artifactId);
            return false;
        }
        log.info("Artifact validation passed: id={}, name={}", artifactId, artifact.getName());
        return true;
    }

    @Override
    public List<SfArtifact> listArtifactsByType(String artifactType) {
        if (artifactType == null || artifactType.isBlank()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Artifact type is required");
        }
        String tenantId = TenantContextHolder.getTenantId();
        return baseMapper.selectList(
                new LambdaQueryWrapper<SfArtifact>()
                        .eq(SfArtifact::getArtifactType, artifactType)
                        .eq(tenantId != null, SfArtifact::getTenantId, tenantId)
                        .orderByDesc(SfArtifact::getCreatedAt));
    }

    @Override
    public SfArtifact archiveArtifact(Long artifactId) {
        SfArtifact artifact = baseMapper.selectById(artifactId);
        if (artifact == null) {
            throw new BaseException(ResultCode.NOT_FOUND, "Artifact not found: " + artifactId);
        }
        if (artifact.getStatus() != null && artifact.getStatus() == STATUS_ARCHIVED) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Artifact is already archived: " + artifactId);
        }
        artifact.setStatus(STATUS_ARCHIVED);
        baseMapper.updateById(artifact);
        log.info("Archived artifact: id={}, name={}", artifactId, artifact.getName());
        return artifact;
    }
}
