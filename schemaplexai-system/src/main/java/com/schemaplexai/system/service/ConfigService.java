package com.schemaplexai.system.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.schemaplexai.system.entity.SfConfig;
import com.schemaplexai.system.mapper.SfConfigMapper;
import org.springframework.stereotype.Service;

@Service
public class ConfigService extends ServiceImpl<SfConfigMapper, SfConfig> {

    public String getConfigValue(String configKey, String tenantId) {
        SfConfig config = baseMapper.selectByKeyAndTenantId(configKey, tenantId);
        return config != null ? config.getConfigValue() : null;
    }
}
