package com.schemaplexai.spec.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.schemaplexai.common.context.TenantContextHolder;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
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
    public SfSpecVersion publishSpec(Long specId) {
        SfSpec spec = specMapper.selectById(specId);
        if (spec == null) {
            throw new BaseException(ResultCode.SPEC_NOT_FOUND);
        }

        spec.setStatus("published");
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
        SfSpec spec = specMapper.selectById(specId);
        if (spec == null) {
            throw new BaseException(ResultCode.SPEC_NOT_FOUND);
        }
        spec.setStatus("archived");
        spec.setUpdatedAt(LocalDateTime.now());
        int rows = specMapper.updateById(spec);
        log.info("Archived spec {}", specId);
        return rows > 0;
    }

    @Override
    public Optional<SfSpecVersion> getLatestVersion(Long specId) {
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
        SfSpecTemplate template = specTemplateMapper.selectById(templateId);
        if (template == null) {
            throw new BaseException(ResultCode.NOT_FOUND, "Template not found");
        }

        SfSpec spec = new SfSpec();
        spec.setTitle(title);
        spec.setType(type);
        spec.setStatus("draft");
        spec.setContent(template.getContent());
        specMapper.insert(spec);

        log.info("Created spec {} from template {}", spec.getId(), templateId);
        return spec;
    }
}
