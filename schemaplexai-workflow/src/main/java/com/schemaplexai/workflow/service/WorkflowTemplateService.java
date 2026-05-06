package com.schemaplexai.workflow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.schemaplexai.workflow.entity.SfWorkflowTemplate;

import java.util.List;

public interface WorkflowTemplateService extends IService<SfWorkflowTemplate> {

    /**
     * Deploy a workflow template, changing its status to deployed.
     *
     * @param templateId the template ID
     * @return the deployed template
     */
    SfWorkflowTemplate deployTemplate(Long templateId);

    /**
     * Validate a workflow template's node configuration.
     *
     * @param templateId the template ID
     * @return true if valid
     */
    boolean validateTemplate(Long templateId);

    /**
     * Clone an existing workflow template.
     *
     * @param templateId the source template ID
     * @param newName    the name for the cloned template
     * @return the cloned template
     */
    SfWorkflowTemplate cloneTemplate(Long templateId, String newName);

    /**
     * List all deployed workflow templates.
     *
     * @return list of deployed templates
     */
    List<SfWorkflowTemplate> listDeployedTemplates();

    /**
     * Deactivate a deployed workflow template.
     *
     * @param templateId the template ID
     * @return the deactivated template
     */
    SfWorkflowTemplate deactivateTemplate(Long templateId);
}
