package com.schemaplexai.agent.config.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.schemaplexai.agent.config.mapper.SfAgentShadowConfigMapper;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.model.entity.agent.SfAgentShadowConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShadowConfigServiceTest {

    @Mock
    private SfAgentShadowConfigMapper shadowConfigMapper;

    @InjectMocks
    private ShadowConfigService shadowConfigService;

    @Test
    void shouldGetByAgentId() {
        SfAgentShadowConfig config = new SfAgentShadowConfig();
        config.setAgentId(1L);
        config.setEnabled(true);
        when(shadowConfigMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(config);

        SfAgentShadowConfig result = shadowConfigService.getByAgentId(1L);

        assertThat(result).isNotNull();
        assertThat(result.getAgentId()).isEqualTo(1L);
        assertThat(result.getEnabled()).isTrue();
    }

    @Test
    void shouldReturnNullWhenGetByAgentIdNotFound() {
        when(shadowConfigMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        SfAgentShadowConfig result = shadowConfigService.getByAgentId(1L);

        assertThat(result).isNull();
    }

    @Test
    void shouldListShadowConfigs() {
        SfAgentShadowConfig config = new SfAgentShadowConfig();
        config.setAgentId(1L);
        when(shadowConfigMapper.selectList(null)).thenReturn(List.of(config));

        List<SfAgentShadowConfig> result = shadowConfigService.listShadowConfigs();

        assertThat(result).hasSize(1);
    }

    @Test
    void shouldCreateShadowConfig() {
        SfAgentShadowConfig config = new SfAgentShadowConfig();
        config.setAgentId(1L);
        config.setEnabled(true);
        when(shadowConfigMapper.insert(config)).thenReturn(1);

        shadowConfigService.createShadowConfig(config);

        verify(shadowConfigMapper).insert(config);
    }

    @Test
    void shouldUpdateShadowConfig() {
        SfAgentShadowConfig existing = new SfAgentShadowConfig();
        existing.setId(1L);
        existing.setAgentId(1L);
        existing.setEnabled(false);

        SfAgentShadowConfig update = new SfAgentShadowConfig();
        update.setId(1L);
        update.setAgentId(1L);
        update.setEnabled(true);

        when(shadowConfigMapper.selectById(1L)).thenReturn(existing);
        when(shadowConfigMapper.updateById(update)).thenReturn(1);

        shadowConfigService.updateShadowConfig(update);

        verify(shadowConfigMapper).updateById(update);
    }

    @Test
    void shouldThrowNotFoundWhenUpdateShadowConfigWithInvalidId() {
        SfAgentShadowConfig config = new SfAgentShadowConfig();
        config.setId(999L);
        when(shadowConfigMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> shadowConfigService.updateShadowConfig(config))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void shouldDeleteShadowConfig() {
        when(shadowConfigMapper.deleteById(1L)).thenReturn(1);

        shadowConfigService.deleteShadowConfig(1L);

        verify(shadowConfigMapper).deleteById(1L);
    }
}
