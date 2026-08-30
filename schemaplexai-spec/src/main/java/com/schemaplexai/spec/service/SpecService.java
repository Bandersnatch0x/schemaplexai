package com.schemaplexai.spec.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.schemaplexai.spec.entity.SfSpec;
import com.schemaplexai.spec.entity.SfSpecVersion;

import java.util.List;
import java.util.Optional;

public interface SpecService extends IService<SfSpec> {

    /**
     * Create a new spec. The lifecycle always starts in "draft" regardless of
     * any client-supplied status, so non-draft states cannot be bypassed at
     * creation time.
     *
     * @param spec the spec to create
     * @return the created spec with generated id
     */
    SfSpec createSpec(SfSpec spec);

    /**
     * Update a spec. Only specs in "draft" status are editable
     * (spec-management §3.1); editing any other status is rejected with
     * FORBIDDEN. The status field itself is a lifecycle field and cannot be
     * rewritten through a plain update.
     *
     * @param id     the spec id
     * @param update the fields to change (null fields are left untouched)
     * @return true if the update was applied
     */
    boolean updateSpec(Long id, SfSpec update);

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
     * Roll a spec back to a historical version snapshot (spec-management §3.2
     * "rollback", §6.1 {@code POST /spec/specs/{id}/rollback}). The snapshot's
     * content is restored into the spec as a new editable {@code draft}; the
     * historical snapshot itself is never mutated (snapshots are immutable).
     * Distinct from {@link #archiveSpec(Long)}: rollback returns the document
     * to an active working state, archive removes it from the active lifecycle.
     *
     * @param specId    the spec id
     * @param versionId the id of the historical snapshot to restore
     * @return the restored spec (status {@code draft})
     */
    SfSpec rollbackSpec(Long specId, Long versionId);

    /**
     * Delete a spec (logical delete) and record a DELETE row in the change
     * audit trail (spec-management §4.3, REQ-12).
     *
     * @param id the spec id
     * @return true if a row was deleted
     */
    boolean deleteSpec(Long id);

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
