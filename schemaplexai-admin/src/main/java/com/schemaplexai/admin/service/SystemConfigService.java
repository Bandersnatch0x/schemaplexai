package com.schemaplexai.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.schemaplexai.admin.dto.SystemConfigDTO;
import com.schemaplexai.admin.dto.SystemConfigQuery;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.model.dto.PageResult;
import com.schemaplexai.system.entity.SfConfig;
import com.schemaplexai.system.mapper.SfConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemConfigService extends ServiceImpl<SfConfigMapper, SfConfig> {

    private static final List<String> RESERVED_KEYS = List.of(
            "system.maintenance.mode",
            "system.rate.limit.global",
            "system.rate.limit.per.tenant",
            "system.feature.agent.enabled",
            "system.feature.workflow.enabled",
            "system.feature.integration.enabled"
    );

    public PageResult<SystemConfigDTO> queryConfigs(SystemConfigQuery query) {
        Page<SfConfig> page = lambdaQuery()
                .eq(query.getTenantId() != null && !query.getTenantId().isBlank(), SfConfig::getTenantId, query.getTenantId())
                .like(query.getConfigKey() != null && !query.getConfigKey().isBlank(), SfConfig::getConfigKey, query.getConfigKey())
                .orderByAsc(SfConfig::getConfigKey)
                .page(new Page<>(query.getCurrent(), query.getSize()));

        List<SystemConfigDTO> dtos = page.getRecords().stream()
                .map(this::toDTO)
                .toList();

        return PageResult.of(dtos, page.getTotal(), page.getCurrent(), page.getSize());
    }

    public SystemConfigDTO getConfigDetail(Long id) {
        SfConfig config = getById(id);
        if (config == null) {
            throw new BaseException(ResultCode.NOT_FOUND, "config not found");
        }
        return toDTO(config);
    }

    public SystemConfigDTO getConfigByKey(String configKey, String tenantId) {
        SfConfig config = baseMapper.selectByKeyAndTenantId(configKey, tenantId);
        if (config == null) {
            throw new BaseException(ResultCode.NOT_FOUND, "config not found");
        }
        return toDTO(config);
    }

    @Transactional
    public SystemConfigDTO createConfig(SystemConfigDTO dto) {
        validateConfigKey(dto.getConfigKey());

        SfConfig existing = baseMapper.selectByKeyAndTenantId(dto.getConfigKey(), dto.getTenantId());
        if (existing != null) {
            throw new BaseException(ResultCode.PARAM_ERROR, "config key already exists for this tenant");
        }

        SfConfig config = new SfConfig();
        config.setTenantId(dto.getTenantId());
        config.setConfigKey(dto.getConfigKey());
        config.setConfigValue(dto.getConfigValue());
        save(config);

        log.info("System config created: key={}, tenantId={}", dto.getConfigKey(), dto.getTenantId());
        return toDTO(config);
    }

    @Transactional
    public SystemConfigDTO updateConfig(Long id, SystemConfigDTO dto) {
        SfConfig config = getById(id);
        if (config == null) {
            throw new BaseException(ResultCode.NOT_FOUND, "config not found");
        }

        config.setConfigValue(dto.getConfigValue());
        updateById(config);

        log.info("System config updated: id={}, key={}", id, config.getConfigKey());
        return toDTO(config);
    }

    @Transactional
    public void deleteConfig(Long id) {
        SfConfig config = getById(id);
        if (config == null) {
            throw new BaseException(ResultCode.NOT_FOUND, "config not found");
        }
        removeById(id);
        log.info("System config deleted: id={}, key={}", id, config.getConfigKey());
    }

    @Transactional
    public void setMaintenanceMode(boolean enabled, String tenantId) {
        setConfigValue("system.maintenance.mode", enabled ? "true" : "false", tenantId);
        log.info("Maintenance mode set to {} for tenantId={}", enabled, tenantId);
    }

    @Transactional
    public void setFeatureFlag(String featureKey, boolean enabled, String tenantId) {
        String configKey = "system.feature." + featureKey + ".enabled";
        setConfigValue(configKey, enabled ? "true" : "false", tenantId);
        log.info("Feature flag {} set to {} for tenantId={}", featureKey, enabled, tenantId);
    }

    public boolean isMaintenanceMode(String tenantId) {
        SfConfig config = baseMapper.selectByKeyAndTenantId("system.maintenance.mode", tenantId);
        return config != null && "true".equalsIgnoreCase(config.getConfigValue());
    }

    public boolean isFeatureEnabled(String featureKey, String tenantId) {
        String configKey = "system.feature." + featureKey + ".enabled";
        SfConfig config = baseMapper.selectByKeyAndTenantId(configKey, tenantId);
        return config == null || "true".equalsIgnoreCase(config.getConfigValue());
    }

    private void setConfigValue(String configKey, String configValue, String tenantId) {
        SfConfig config = baseMapper.selectByKeyAndTenantId(configKey, tenantId);
        if (config != null) {
            config.setConfigValue(configValue);
            updateById(config);
        } else {
            config = new SfConfig();
            config.setTenantId(tenantId);
            config.setConfigKey(configKey);
            config.setConfigValue(configValue);
            save(config);
        }
    }

    private void validateConfigKey(String configKey) {
        if (configKey == null || configKey.isBlank()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "config key is required");
        }
        if (!configKey.matches("^[a-z0-9.]+$")) {
            throw new BaseException(ResultCode.PARAM_ERROR, "config key must contain only lowercase letters, numbers, and dots");
        }
    }

    private SystemConfigDTO toDTO(SfConfig config) {
        SystemConfigDTO dto = new SystemConfigDTO();
        dto.setId(config.getId());
        dto.setTenantId(config.getTenantId());
        dto.setConfigKey(config.getConfigKey());
        dto.setConfigValue(config.getConfigValue());
        dto.setCreatedAt(config.getCreatedAt());
        dto.setUpdatedAt(config.getUpdatedAt());
        return dto;
    }
}
