package com.schemaplexai.agent.config.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.schemaplexai.agent.config.entity.SfPromptVersion;

import java.util.List;
import java.util.Optional;

public interface PromptVersionService extends IService<SfPromptVersion> {

    SfPromptVersion createVersion(Long configId, Long agentId, String content,
                                   String label, String changeNote);

    Optional<SfPromptVersion> getByLabel(Long configId, String label);

    List<SfPromptVersion> listVersions(Long configId);
}
