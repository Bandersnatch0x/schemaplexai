package com.schemaplexai.agent.config.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.schemaplexai.agent.config.entity.SfAgent;
import com.schemaplexai.agent.config.entity.SfAgentConfig;
import com.schemaplexai.agent.config.entity.SfAgentToolBinding;
import com.schemaplexai.agent.config.manifest.AgentsManifestLoader;
import com.schemaplexai.agent.config.manifest.LoadReport;
import com.schemaplexai.agent.config.mapper.SfAgentConfigMapper;
import com.schemaplexai.agent.config.mapper.SfAgentMapper;
import com.schemaplexai.agent.config.mapper.SfAgentToolBindingMapper;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AgentConfigService {

    private final SfAgentMapper agentMapper;
    private final SfAgentConfigMapper agentConfigMapper;
    private final SfAgentToolBindingMapper toolBindingMapper;
    private final AgentsManifestLoader manifestLoader;

    @Transactional(rollbackFor = Exception.class)
    public LoadReport loadFromManifest(Path repoRoot, String tenantId) {
        return manifestLoader.load(repoRoot, tenantId);
    }

    public SfAgent getAgent(Long id) {
        SfAgent agent = agentMapper.selectById(id);
        if (agent == null) {
            throw new BaseException(ResultCode.AGENT_NOT_FOUND);
        }
        return agent;
    }

    public List<SfAgent> listAgents() {
        return agentMapper.selectList(null);
    }

    @Transactional(rollbackFor = Exception.class)
    public void createAgent(SfAgent agent) {
        agentMapper.insert(agent);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateAgent(SfAgent agent) {
        agentMapper.updateById(agent);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteAgent(Long id) {
        agentMapper.deleteById(id);
    }

    public SfAgentConfig getAgentConfig(Long agentId) {
        return agentConfigMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SfAgentConfig>()
                        .eq(SfAgentConfig::getAgentId, agentId)
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveAgentConfig(SfAgentConfig config) {
        if (config.getId() == null) {
            agentConfigMapper.insert(config);
        } else {
            agentConfigMapper.updateById(config);
        }
    }

    public List<SfAgentToolBinding> listToolBindings(Long agentId) {
        return toolBindingMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SfAgentToolBinding>()
                        .eq(SfAgentToolBinding::getAgentId, agentId)
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveToolBindings(Long agentId, List<SfAgentToolBinding> bindings) {
        toolBindingMapper.delete(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SfAgentToolBinding>()
                        .eq(SfAgentToolBinding::getAgentId, agentId)
        );
        if (bindings != null) {
            for (SfAgentToolBinding binding : bindings) {
                binding.setAgentId(agentId);
                toolBindingMapper.insert(binding);
            }
        }
    }
}
