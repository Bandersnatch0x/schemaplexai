package com.schemaplexai.agent.config.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.schemaplexai.agent.config.entity.SfAgentShadowConfig;
import com.schemaplexai.agent.config.mapper.SfAgentShadowConfigMapper;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ShadowConfigService {

    private final SfAgentShadowConfigMapper shadowConfigMapper;

    public SfAgentShadowConfig getByAgentId(Long agentId) {
        return shadowConfigMapper.selectOne(
                new LambdaQueryWrapper<SfAgentShadowConfig>()
                        .eq(SfAgentShadowConfig::getAgentId, agentId)
        );
    }

    public List<SfAgentShadowConfig> listShadowConfigs() {
        return shadowConfigMapper.selectList(null);
    }

    @Transactional(rollbackFor = Exception.class)
    public void createShadowConfig(SfAgentShadowConfig config) {
        shadowConfigMapper.insert(config);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateShadowConfig(SfAgentShadowConfig config) {
        if (shadowConfigMapper.selectById(config.getId()) == null) {
            throw new BaseException(ResultCode.NOT_FOUND);
        }
        shadowConfigMapper.updateById(config);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteShadowConfig(Long id) {
        shadowConfigMapper.deleteById(id);
    }
}
