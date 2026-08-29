package com.schemaplexai.spec.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.schemaplexai.spec.dto.SpecDiffResult;
import com.schemaplexai.spec.entity.SfSpecVersion;

public interface SpecVersionService extends IService<SfSpecVersion> {

    SpecDiffResult diff(Long versionAId, Long versionBId);

    SfSpecVersion createVersion(Long specId, String version, String content, String changeLog);

    /**
     * Update a version snapshot. Version snapshots are immutable audit
     * records, so updates are always rejected with FORBIDDEN. Create a new
     * version instead of editing a historical snapshot.
     *
     * @param id      the version id
     * @param version the attempted new state (ignored)
     * @return never returns normally; always throws
     */
    boolean updateVersion(Long id, SfSpecVersion version);
}
