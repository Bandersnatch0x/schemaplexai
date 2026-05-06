package com.schemaplexai.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.schemaplexai.common.context.TenantContextHolder;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.workflow.entity.SfWorkflowTemplate;
import com.schemaplexai.workflow.mapper.SfWorkflowTemplateMapper;
import com.schemaplexai.workflow.service.WorkflowTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Transactional(rollbackFor = Exception.class)
@Service
@RequiredArgsConstructor
public class WorkflowTemplateServiceImpl extends ServiceImpl<SfWorkflowTemplateMapper, SfWorkflowTemplate> implements WorkflowTemplateService {

    private static final String STATUS_DRAFT = "draft";
    private static final String STATUS_DEPLOYED = "deployed";
    private static final String STATUS_INACTIVE = "inactive";

    @Override
    public SfWorkflowTemplate deployTemplate(Long templateId) {
        SfWorkflowTemplate template = baseMapper.selectById(templateId);
        if (template == null) {
            throw new BaseException(ResultCode.WORKFLOW_NOT_FOUND, "Workflow template not found: " + templateId);
        }
        if (STATUS_DEPLOYED.equals(template.getStatus())) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Template is already deployed: " + templateId);
        }
        template.setStatus(STATUS_DEPLOYED);
        baseMapper.updateById(template);
        log.info("Deployed workflow template: id={}, name={}", templateId, template.getName());
        return template;
    }

    @Override
    public boolean validateTemplate(Long templateId) {
        SfWorkflowTemplate template = baseMapper.selectById(templateId);
        if (template == null) {
            throw new BaseException(ResultCode.WORKFLOW_NOT_FOUND, "Workflow template not found: " + templateId);
        }
        if (template.getNodeConfigJson() == null || template.getNodeConfigJson().isBlank()) {
            log.warn("Template validation failed - missing node config: id={}", templateId);
            return false;
        }
        if (template.getName() == null || template.getName().isBlank()) {
            log.warn("Template validation failed - missing name: id={}", templateId);
            return false;
        }
        log.info("Template validation passed: id={}, name={}", templateId, template.getName());
        return true;
    }

    @Override
    public SfWorkflowTemplate cloneTemplate(Long templateId, String newName) {
        SfWorkflowTemplate source = baseMapper.selectById(templateId);
        if (source == null) {
            throw new BaseException(ResultCode.WORKFLOW_NOT_FOUND, "Workflow template not found: " + templateId);
        }
        if (newName == null || newName.isBlank()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "New name is required for cloning");
        }
        SfWorkflowTemplate clone = new SfWorkflowTemplate();
        clone.setName(newName);
        clone.setDescription(source.getDescription());
        clone.setNodeConfigJson(source.getNodeConfigJson());
        clone.setStatus(STATUS_DRAFT);
        clone.setTenantId(TenantContextHolder.getTenantId());
        baseMapper.insert(clone);
        log.info("Cloned workflow template: sourceId={}, newId={}, name={}", templateId, clone.getId(), newName);
        return clone;
    }

    @Override
    public List<SfWorkflowTemplate> listDeployedTemplates() {
        String tenantId = TenantContextHolder.getTenantId();
        return baseMapper.selectList(
                new LambdaQueryWrapper<SfWorkflowTemplate>()
                        .eq(SfWorkflowTemplate::getStatus, STATUS_DEPLOYED)
                        .eq(tenantId != null, SfWorkflowTemplate::getTenantId, tenantId)
                        .orderByDesc(SfWorkflowTemplate::getCreatedAt));
    }

    @Override
    public SfWorkflowTemplate deactivateTemplate(Long templateId) {
        SfWorkflowTemplate template = baseMapper.selectById(templateId);
        if (template == null) {
            throw new BaseException(ResultCode.WORKFLOW_NOT_FOUND, "Workflow template not found: " + templateId);
        }
        if (!STATUS_DEPLOYED.equals(template.getStatus())) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Only deployed templates can be deactivated: " + templateId);
        }
        template.setStatus(STATUS_INACTIVE);
        baseMapper.updateById(template);
        log.info("Deactivated workflow template: id={}, name={}", templateId, template.getName());
        return template;
    }
}
