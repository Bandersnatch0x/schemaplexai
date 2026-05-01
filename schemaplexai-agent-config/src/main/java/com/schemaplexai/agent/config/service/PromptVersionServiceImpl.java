package com.schemaplexai.agent.config.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.schemaplexai.agent.config.entity.SfPromptVersion;
import com.schemaplexai.agent.config.mapper.PromptVersionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PromptVersionServiceImpl
        extends ServiceImpl<PromptVersionMapper, SfPromptVersion>
        implements PromptVersionService {

    private final PromptVersionMapper promptVersionMapper;

    @Override
    public SfPromptVersion createVersion(Long configId, Long agentId,
                                          String content, String label, String changeNote) {
        // Atomic version increment using row-level locking (FOR UPDATE).
        // Note: for multi-instance deployments, use a database sequence instead.
        LambdaQueryWrapper<SfPromptVersion> lockWrapper = new LambdaQueryWrapper<>();
        lockWrapper.eq(SfPromptVersion::getConfigId, configId)
                   .orderByDesc(SfPromptVersion::getVersion)
                   .last("FOR UPDATE");
        SfPromptVersion latest = promptVersionMapper.selectOne(lockWrapper);
        int nextVersion = (latest != null && latest.getVersion() != null ? latest.getVersion() : 0) + 1;

        SfPromptVersion pv = new SfPromptVersion();
        pv.setConfigId(configId);
        pv.setAgentId(agentId);
        pv.setVersion(nextVersion);
        pv.setContent(content);
        pv.setLabel(label);
        pv.setChangeNote(changeNote);
        promptVersionMapper.insert(pv);
        return pv;
    }

    @Override
    public Optional<SfPromptVersion> getByLabel(Long configId, String label) {
        SfPromptVersion pv = promptVersionMapper.selectOne(
            new LambdaQueryWrapper<SfPromptVersion>()
                .eq(SfPromptVersion::getConfigId, configId)
                .eq(SfPromptVersion::getLabel, label));
        return Optional.ofNullable(pv);
    }

    @Override
    public List<SfPromptVersion> listVersions(Long configId) {
        return promptVersionMapper.selectList(
            new LambdaQueryWrapper<SfPromptVersion>()
                .eq(SfPromptVersion::getConfigId, configId)
                .orderByDesc(SfPromptVersion::getVersion));
    }
}
