package com.schemaplexai.agent.config.service.impl;

import com.schemaplexai.agent.config.mapper.SfAgentShadowConfigMapper;
import com.schemaplexai.common.context.TenantContextHolder;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.model.entity.agent.SfAgentShadowConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentShadowConfigServiceImplTest {

    @Mock
    private SfAgentShadowConfigMapper shadowConfigMapper;

    @InjectMocks
    private AgentShadowConfigServiceImpl agentShadowConfigService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(agentShadowConfigService, "baseMapper", shadowConfigMapper);
    }

    @Test
    void shouldGetByAgentId() {
        SfAgentShadowConfig config = new SfAgentShadowConfig();
        config.setAgentId(1L);
        config.setEnabled(true);
        when(shadowConfigMapper.selectByAgentId(1L, 10L)).thenReturn(config);

        TenantContextHolder.setTenantId("10");
        SfAgentShadowConfig result = agentShadowConfigService.getByAgentId(1L);
        TenantContextHolder.clear();

        assertThat(result).isNotNull();
        assertThat(result.getAgentId()).isEqualTo(1L);
        assertThat(result.getEnabled()).isTrue();
    }

    @Test
    void shouldGetByAgentIdWithNullTenantId() {
        SfAgentShadowConfig config = new SfAgentShadowConfig();
        config.setAgentId(2L);
        when(shadowConfigMapper.selectByAgentId(2L, null)).thenReturn(config);

        TenantContextHolder.clear();
        SfAgentShadowConfig result = agentShadowConfigService.getByAgentId(2L);

        assertThat(result).isNotNull();
        assertThat(result.getAgentId()).isEqualTo(2L);
    }

    @Test
    void shouldToggleEnabled() {
        SfAgentShadowConfig config = new SfAgentShadowConfig();
        config.setId(1L);
        config.setEnabled(false);
        when(shadowConfigMapper.selectById(1L)).thenReturn(config);
        when(shadowConfigMapper.updateById(any(SfAgentShadowConfig.class))).thenReturn(1);

        agentShadowConfigService.toggleEnabled(1L, true);

        assertThat(config.getEnabled()).isTrue();
        verify(shadowConfigMapper).updateById(config);
    }

    @Test
    void shouldThrowNotFoundWhenToggleEnabledWithInvalidId() {
        when(shadowConfigMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> agentShadowConfigService.toggleEnabled(999L, true))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void shouldCreateConfig() {
        SfAgentShadowConfig config = new SfAgentShadowConfig();
        config.setAgentId(1L);
        config.setFeedbackActionsJson("[{\"action\":\"approve\"}]");
        config.setEnabled(true);
        when(shadowConfigMapper.insert(any(SfAgentShadowConfig.class))).thenReturn(1);

        boolean result = agentShadowConfigService.save(config);

        assertThat(result).isTrue();
        verify(shadowConfigMapper).insert(config);
    }

    @Test
    void shouldUpdateConfig() {
        SfAgentShadowConfig config = new SfAgentShadowConfig();
        config.setId(1L);
        config.setAgentId(1L);
        config.setFeedbackActionsJson("[{\"action\":\"reject\"}]");
        config.setEnabled(false);
        when(shadowConfigMapper.updateById(any(SfAgentShadowConfig.class))).thenReturn(1);

        boolean result = agentShadowConfigService.updateById(config);

        assertThat(result).isTrue();
        verify(shadowConfigMapper).updateById(config);
    }
}
