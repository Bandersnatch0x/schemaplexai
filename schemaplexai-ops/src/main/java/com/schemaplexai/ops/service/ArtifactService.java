package com.schemaplexai.ops.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.schemaplexai.ops.entity.SfArtifact;

import java.util.List;

public interface ArtifactService extends IService<SfArtifact> {

    /**
     * Upload a new artifact with validation.
     *
     * @param artifact the artifact to upload
     * @return the uploaded artifact
     */
    SfArtifact uploadArtifact(SfArtifact artifact);

    /**
     * Download an artifact by ID.
     *
     * @param artifactId the artifact ID
     * @return the artifact
     */
    SfArtifact downloadArtifact(Long artifactId);

    /**
     * Validate an artifact's integrity.
     *
     * @param artifactId the artifact ID
     * @return true if valid
     */
    boolean validateArtifact(Long artifactId);

    /**
     * List artifacts by type.
     *
     * @param artifactType the artifact type
     * @return list of matching artifacts
     */
    List<SfArtifact> listArtifactsByType(String artifactType);

    /**
     * Archive an artifact.
     *
     * @param artifactId the artifact ID
     * @return the archived artifact
     */
    SfArtifact archiveArtifact(Long artifactId);
}
