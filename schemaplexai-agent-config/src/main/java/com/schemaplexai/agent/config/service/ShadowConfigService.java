package com.schemaplexai.agent.config.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.schemaplexai.model.entity.agent.SfAgentShadowConfig;
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
        int inserted = shadowConfigMapper.insert(config);
        if (inserted <= 0) {
            throw new BaseException(ResultCode.INTERNAL_ERROR);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateShadowConfig(SfAgentShadowConfig config) {
        if (shadowConfigMapper.selectById(config.getId()) == null) {
            throw new BaseException(ResultCode.NOT_FOUND);
        }
        int updated = shadowConfigMapper.updateById(config);
        if (updated <= 0) {
            throw new BaseException(ResultCode.NOT_FOUND);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteShadowConfig(Long id) {
        int deleted = shadowConfigMapper.deleteById(id);
        if (deleted <= 0) {
            throw new BaseException(ResultCode.NOT_FOUND);
        }
    }
}
