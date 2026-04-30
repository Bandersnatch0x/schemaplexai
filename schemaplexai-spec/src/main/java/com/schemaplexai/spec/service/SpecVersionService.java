package com.schemaplexai.spec.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.schemaplexai.spec.dto.SpecDiffResult;
import com.schemaplexai.spec.entity.SfSpecVersion;

public interface SpecVersionService extends IService<SfSpecVersion> {

    SpecDiffResult diff(Long versionAId, Long versionBId);

    SfSpecVersion createVersion(Long specId, String version, String content, String changeLog);
}
