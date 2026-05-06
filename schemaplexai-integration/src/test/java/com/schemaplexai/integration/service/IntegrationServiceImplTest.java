package com.schemaplexai.integration.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.integration.entity.SfIntegration;
import com.schemaplexai.integration.mapper.IntegrationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IntegrationServiceImplTest {

    @Mock
    private IntegrationMapper integrationMapper;

    @InjectMocks
    private IntegrationServiceImpl integrationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(integrationService, "baseMapper", integrationMapper);
        ReflectionTestUtils.setField(integrationService, "objectMapper", new ObjectMapper());
    }

    // ------------------------------------------------------------------
    // save
    // ------------------------------------------------------------------

    @Test
    void save_nullName_throwsParamError() {
        SfIntegration entity = new SfIntegration();
        entity.setType("github");

        assertThatThrownBy(() -> integrationService.save(entity))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void save_nullType_throwsParamError() {
        SfIntegration entity = new SfIntegration();
        entity.setName("GitHub");

        assertThatThrownBy(() -> integrationService.save(entity))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void save_success() {
        SfIntegration entity = new SfIntegration();
        entity.setName("GitHub");
        entity.setType("github");
        when(integrationMapper.insert(any())).thenReturn(1);

        boolean result = integrationService.save(entity);

        assertThat(result).isTrue();
    }

    // ------------------------------------------------------------------
    // registerWebhook
    // ------------------------------------------------------------------

    @Test
    void registerWebhook_notFound_throwsNotFound() {
        when(integrationMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> integrationService.registerWebhook(1L, "https://example.com/hook", "push"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.INTEGRATION_NOT_FOUND.getCode());
    }

    @Test
    void registerWebhook_nullUrl_throwsParamError() {
        SfIntegration entity = new SfIntegration();
        entity.setId(1L);
        when(integrationMapper.selectById(1L)).thenReturn(entity);

        assertThatThrownBy(() -> integrationService.registerWebhook(1L, null, "push"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void registerWebhook_success_addsToList() {
        SfIntegration entity = new SfIntegration();
        entity.setId(1L);
        when(integrationMapper.selectById(1L)).thenReturn(entity);

        integrationService.registerWebhook(1L, "https://example.com/hook", "push");

        List<Map<String, Object>> webhooks = integrationService.listWebhooks(1L);
        assertThat(webhooks).hasSize(1);
        assertThat(webhooks.get(0).get("url")).isEqualTo("https://example.com/hook");
    }

    // ------------------------------------------------------------------
    // listWebhooks
    // ------------------------------------------------------------------

    @Test
    void listWebhooks_notFound_throwsNotFound() {
        when(integrationMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> integrationService.listWebhooks(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.INTEGRATION_NOT_FOUND.getCode());
    }

    @Test
    void listWebhooks_empty_returnsEmptyList() {
        SfIntegration entity = new SfIntegration();
        entity.setId(1L);
        when(integrationMapper.selectById(1L)).thenReturn(entity);

        List<Map<String, Object>> result = integrationService.listWebhooks(1L);

        assertThat(result).isEmpty();
    }

    // ------------------------------------------------------------------
    // deleteWebhook
    // ------------------------------------------------------------------

    @Test
    void deleteWebhook_notFound_throwsNotFound() {
        assertThatThrownBy(() -> integrationService.deleteWebhook(999L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void deleteWebhook_success_removesWebhook() {
        SfIntegration entity = new SfIntegration();
        entity.setId(1L);
        when(integrationMapper.selectById(1L)).thenReturn(entity);

        integrationService.registerWebhook(1L, "https://example.com/hook", "push");
        List<Map<String, Object>> webhooks = integrationService.listWebhooks(1L);
        Long webhookId = (Long) webhooks.get(0).get("id");

        integrationService.deleteWebhook(webhookId);

        assertThat(integrationService.listWebhooks(1L)).isEmpty();
    }

    // ------------------------------------------------------------------
    // aggregateHealthStatus
    // ------------------------------------------------------------------

    @Test
    void aggregateHealthStatus_noIntegrations_returnsZeroCounts() {
        when(integrationMapper.selectList(any())).thenReturn(Collections.emptyList());

        Map<String, Object> result = integrationService.aggregateHealthStatus();

        assertThat(result.get("total")).isEqualTo(0);
        assertThat(result.get("active")).isEqualTo(0);
    }

    @Test
    void aggregateHealthStatus_mixedStatus_returnsCorrectCounts() {
        SfIntegration active = new SfIntegration();
        active.setId(1L);
        active.setName("A");
        active.setType("github");
        active.setStatus(1);
        SfIntegration inactive = new SfIntegration();
        inactive.setId(2L);
        inactive.setName("B");
        inactive.setType("jenkins");
        inactive.setStatus(0);
        when(integrationMapper.selectList(any())).thenReturn(List.of(active, inactive));

        Map<String, Object> result = integrationService.aggregateHealthStatus();

        assertThat(result.get("total")).isEqualTo(2);
        assertThat(result.get("active")).isEqualTo(1);
        assertThat(result.get("inactive")).isEqualTo(1);
    }
}
