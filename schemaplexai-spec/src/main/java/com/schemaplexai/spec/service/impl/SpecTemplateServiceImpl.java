package com.schemaplexai.spec.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.schemaplexai.common.context.TenantContextHolder;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.spec.entity.SfSpec;
import com.schemaplexai.spec.entity.SfSpecTemplate;
import com.schemaplexai.spec.mapper.SfSpecMapper;
import com.schemaplexai.spec.mapper.SfSpecTemplateMapper;
import com.schemaplexai.spec.service.SpecTemplateService;
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
public class SpecTemplateServiceImpl extends ServiceImpl<SfSpecTemplateMapper, SfSpecTemplate> implements SpecTemplateService {

    private final SfSpecTemplateMapper specTemplateMapper;
    private final SfSpecMapper specMapper;

    @Override
    public SfSpec applyTemplate(Long templateId, Long specId, String title, String type) {
        SfSpecTemplate template = specTemplateMapper.selectById(templateId);
        if (template == null) {
            throw new BaseException(ResultCode.NOT_FOUND, "Template not found");
        }

        SfSpec spec;
        if (specId != null) {
            spec = specMapper.selectById(specId);
            if (spec == null) {
                throw new BaseException(ResultCode.SPEC_NOT_FOUND);
            }
            spec.setContent(template.getContent());
            spec.setUpdatedAt(LocalDateTime.now());
            specMapper.updateById(spec);
            log.info("Applied template {} to spec {}", templateId, specId);
        } else {
            spec = new SfSpec();
            spec.setTitle(title);
            spec.setType(type);
            spec.setStatus("draft");
            spec.setContent(template.getContent());
            specMapper.insert(spec);
            log.info("Created spec {} from template {}", spec.getId(), templateId);
        }
        return spec;
    }

    @Override
    public Optional<SfSpecTemplate> getDefaultTemplate(String category) {
        String tenantId = TenantContextHolder.getTenantId();
        LambdaQueryWrapper<SfSpecTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SfSpecTemplate::getCategory, category);
        if (tenantId != null) {
            wrapper.eq(SfSpecTemplate::getTenantId, tenantId);
        }
        wrapper.orderByAsc(SfSpecTemplate::getCreatedAt);
        SfSpecTemplate template = specTemplateMapper.selectOne(wrapper);
        return Optional.ofNullable(template);
    }

    @Override
    public List<SfSpecTemplate> listTemplatesByCategory(String category) {
        String tenantId = TenantContextHolder.getTenantId();
        LambdaQueryWrapper<SfSpecTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SfSpecTemplate::getCategory, category);
        if (tenantId != null) {
            wrapper.eq(SfSpecTemplate::getTenantId, tenantId);
        }
        wrapper.orderByAsc(SfSpecTemplate::getName);
        return specTemplateMapper.selectList(wrapper);
    }

    @Override
    public SfSpecTemplate cloneTemplate(Long templateId, String newName) {
        SfSpecTemplate source = specTemplateMapper.selectById(templateId);
        if (source == null) {
            throw new BaseException(ResultCode.NOT_FOUND, "Template not found");
        }

        SfSpecTemplate clone = new SfSpecTemplate();
        clone.setName(newName);
        clone.setContent(source.getContent());
        clone.setCategory(source.getCategory());
        specTemplateMapper.insert(clone);

        log.info("Cloned template {} to new template {}", templateId, clone.getId());
        return clone;
    }
}
