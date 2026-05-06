package com.schemaplexai.spec.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.schemaplexai.spec.entity.SfSpec;
import com.schemaplexai.spec.entity.SfSpecTemplate;

import java.util.List;
import java.util.Optional;

public interface SpecTemplateService extends IService<SfSpecTemplate> {

    /**
     * Apply a template to create or update a spec.
     *
     * @param templateId the template id
     * @param specId     the target spec id (nullable to create new)
     * @param title      the spec title
     * @param type       the spec type
     * @return the resulting spec
     */
    SfSpec applyTemplate(Long templateId, Long specId, String title, String type);

    /**
     * Get the default template for a category.
     *
     * @param category the template category
     * @return optional default template
     */
    Optional<SfSpecTemplate> getDefaultTemplate(String category);

    /**
     * List templates by category.
     *
     * @param category the template category
     * @return list of templates
     */
    List<SfSpecTemplate> listTemplatesByCategory(String category);

    /**
     * Clone an existing template.
     *
     * @param templateId the source template id
     * @param newName    the cloned template name
     * @return the cloned template
     */
    SfSpecTemplate cloneTemplate(Long templateId, String newName);
}
