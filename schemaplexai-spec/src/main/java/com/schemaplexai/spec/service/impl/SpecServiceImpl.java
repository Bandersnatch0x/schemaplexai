package com.schemaplexai.spec.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.schemaplexai.common.context.TenantContextHolder;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.spec.domain.SpecStatus;
import com.schemaplexai.spec.entity.SfSpec;
import com.schemaplexai.spec.entity.SfSpecTemplate;
import com.schemaplexai.spec.entity.SfSpecVersion;
import com.schemaplexai.spec.mapper.SfSpecMapper;
import com.schemaplexai.spec.mapper.SfSpecTemplateMapper;
import com.schemaplexai.spec.mapper.SfSpecVersionMapper;
import com.schemaplexai.spec.service.SpecService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Transactional(rollbackFor = Exception.class)
@Service
@RequiredArgsConstructor
public class SpecServiceImpl extends ServiceImpl<SfSpecMapper, SfSpec> implements SpecService {

    private final SfSpecMapper specMapper;
    private final SfSpecVersionMapper specVersionMapper;
    private final SfSpecTemplateMapper specTemplateMapper;

    @Override
    public SfSpec createSpec(SfSpec spec) {
        if (spec == null) {
            throw new BaseException(ResultCode.PARAM_ERROR, "spec is required");
        }
        // Lifecycle always starts in draft; a client-supplied status cannot
        // shortcut the draft -> in_review -> approved -> published chain.
        spec.setStatus(SpecStatus.DRAFT);
        specMapper.insert(spec);
        log.info("Created spec {} in status draft", spec.getId());
        return spec;
    }

    @Override
    public boolean updateSpec(Long id, SfSpec update) {
        if (id == null) {
            throw new BaseException(ResultCode.PARAM_ERROR, "specId is required");
        }
        if (update == null) {
            throw new BaseException(ResultCode.PARAM_ERROR, "spec is required");
        }
        SfSpec existing = specMapper.selectById(id);
        if (existing == null) {
            throw new BaseException(ResultCode.SPEC_NOT_FOUND);
        }
        // Edit guard (spec-management §3.1): only drafts are editable.
        if (!SpecStatus.isEditable(existing.getStatus())) {
            throw new BaseException(ResultCode.FORBIDDEN,
                    "Spec " + id + " is not editable in status " + existing.getStatus());
        }
        if (update.getTitle() != null) {
            existing.setTitle(update.getTitle());
        }
        if (update.getType() != null) {
            existing.setType(update.getType());
        }
        if (update.getContent() != null) {
            existing.setContent(update.getContent());
        }
        // status is a lifecycle field: never rewritten by a plain update.
        existing.setUpdatedAt(LocalDateTime.now());
        int rows = specMapper.updateById(existing);
        return rows > 0;
    }

    @Override
    public SfSpecVersion publishSpec(Long specId) {
        validateSpecId(specId);
        SfSpec spec = specMapper.selectById(specId);
        if (spec == null) {
            throw new BaseException(ResultCode.SPEC_NOT_FOUND);
        }

        spec.setStatus(SpecStatus.PUBLISHED);
        spec.setUpdatedAt(LocalDateTime.now());
        specMapper.updateById(spec);

        String tenantId = TenantContextHolder.getTenantId();
        LambdaQueryWrapper<SfSpecVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SfSpecVersion::getSpecId, specId);
        if (tenantId != null) {
            wrapper.eq(SfSpecVersion::getTenantId, tenantId);
        }
        wrapper.orderByDesc(SfSpecVersion::getVersion);
        SfSpecVersion latest = specVersionMapper.selectOne(wrapper);

        int nextVersionNumber = 1;
        if (latest != null && latest.getVersion() != null) {
            try {
                nextVersionNumber = Integer.parseInt(latest.getVersion()) + 1;
            } catch (NumberFormatException e) {
                nextVersionNumber = 1;
            }
        }
        String nextVersion = String.valueOf(nextVersionNumber);

        SfSpecVersion version = new SfSpecVersion();
        version.setSpecId(specId);
        version.setVersion(nextVersion);
        version.setContent(spec.getContent());
        version.setChangeLog("Published version " + nextVersion);
        specVersionMapper.insert(version);

        log.info("Published spec {} with version {}", specId, nextVersion);
        return version;
    }

    @Override
    public boolean archiveSpec(Long specId) {
        validateSpecId(specId);
        SfSpec spec = specMapper.selectById(specId);
        if (spec == null) {
            throw new BaseException(ResultCode.SPEC_NOT_FOUND);
        }
        spec.setStatus(SpecStatus.ARCHIVED);
        spec.setUpdatedAt(LocalDateTime.now());
        int rows = specMapper.updateById(spec);
        log.info("Archived spec {}", specId);
        return rows > 0;
    }

    @Override
    public Optional<SfSpecVersion> getLatestVersion(Long specId) {
        validateSpecId(specId);
        String tenantId = TenantContextHolder.getTenantId();
        LambdaQueryWrapper<SfSpecVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SfSpecVersion::getSpecId, specId);
        if (tenantId != null) {
            wrapper.eq(SfSpecVersion::getTenantId, tenantId);
        }
        wrapper.orderByDesc(SfSpecVersion::getVersion);
        SfSpecVersion latest = specVersionMapper.selectOne(wrapper);
        return Optional.ofNullable(latest);
    }

    @Override
    public List<SfSpecVersion> compareVersions(Long specId, String versionA, String versionB) {
        validateSpecId(specId);
        validateVersion(versionA, "versionA");
        validateVersion(versionB, "versionB");
        String tenantId = TenantContextHolder.getTenantId();
        LambdaQueryWrapper<SfSpecVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SfSpecVersion::getSpecId, specId);
        if (tenantId != null) {
            wrapper.eq(SfSpecVersion::getTenantId, tenantId);
        }
        wrapper.and(w -> w.eq(SfSpecVersion::getVersion, versionA).or().eq(SfSpecVersion::getVersion, versionB));
        return specVersionMapper.selectList(wrapper);
    }

    @Override
    public SfSpec createFromTemplate(Long templateId, String title, String type) {
        validateTemplateId(templateId);
        validateSpecTitle(title);
        validateSpecType(type);
        SfSpecTemplate template = specTemplateMapper.selectById(templateId);
        if (template == null) {
            throw new BaseException(ResultCode.NOT_FOUND, "Template not found");
        }

        SfSpec spec = new SfSpec();
        spec.setTitle(title);
        spec.setType(type);
        spec.setStatus(SpecStatus.DRAFT);
        spec.setContent(template.getContent());
        specMapper.insert(spec);

        log.info("Created spec {} from template {}", spec.getId(), templateId);
        return spec;
    }

    private void validateSpecId(Long specId) {
        if (specId == null) {
            throw new BaseException(ResultCode.PARAM_ERROR, "specId is required");
        }
    }

    private void validateTemplateId(Long templateId) {
        if (templateId == null) {
            throw new BaseException(ResultCode.PARAM_ERROR, "templateId is required");
        }
    }

    private void validateSpecTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "title is required");
        }
    }

    private void validateSpecType(String type) {
        if (type == null || type.isBlank()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "type is required");
        }
    }

    private void validateVersion(String version, String fieldName) {
        if (version == null || version.isBlank()) {
            throw new BaseException(ResultCode.PARAM_ERROR, fieldName + " is required");
        }
    }
}
