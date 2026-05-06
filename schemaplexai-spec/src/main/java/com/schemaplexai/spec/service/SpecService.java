package com.schemaplexai.spec.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.schemaplexai.spec.entity.SfSpec;
import com.schemaplexai.spec.entity.SfSpecVersion;

import java.util.List;
import java.util.Optional;

public interface SpecService extends IService<SfSpec> {

    /**
     * Publish a spec by changing its status to "published" and creating a version snapshot.
     *
     * @param specId the spec id
     * @return the created version
     */
    SfSpecVersion publishSpec(Long specId);

    /**
     * Archive a spec by changing its status to "archived".
     *
     * @param specId the spec id
     * @return true if archived
     */
    boolean archiveSpec(Long specId);

    /**
     * Get the latest version for a spec.
     *
     * @param specId the spec id
     * @return optional latest version
     */
    Optional<SfSpecVersion> getLatestVersion(Long specId);

    /**
     * Compare two versions of a spec.
     *
     * @param specId    the spec id
     * @param versionA  first version string
     * @param versionB  second version string
     * @return pair of contents (versionA content, versionB content)
     */
    List<SfSpecVersion> compareVersions(Long specId, String versionA, String versionB);

    /**
     * Create a new spec from a template.
     *
     * @param templateId the template id
     * @param title      the new spec title
     * @param type       the new spec type
     * @return the created spec
     */
    SfSpec createFromTemplate(Long templateId, String title, String type);
}
