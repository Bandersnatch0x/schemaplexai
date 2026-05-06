package com.schemaplexai.integration.service;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.integration.entity.SfApiGatewayConfig;
import com.schemaplexai.integration.mapper.ApiGatewayConfigMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiGatewayServiceImplTest {

    @Mock
    private ApiGatewayConfigMapper apiGatewayConfigMapper;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private ApiGatewayServiceImpl apiGatewayService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(apiGatewayService, "baseMapper", apiGatewayConfigMapper);
    }

    // ------------------------------------------------------------------
    // save
    // ------------------------------------------------------------------

    @Test
    void save_nullName_throwsParamError() {
        SfApiGatewayConfig config = new SfApiGatewayConfig();
        config.setBaseUrl("http://localhost:8080");

        assertThatThrownBy(() -> apiGatewayService.save(config))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void save_nullBaseUrl_throwsParamError() {
        SfApiGatewayConfig config = new SfApiGatewayConfig();
        config.setName("Gateway");

        assertThatThrownBy(() -> apiGatewayService.save(config))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void save_invalidRateLimit_throwsParamError() {
        SfApiGatewayConfig config = new SfApiGatewayConfig();
        config.setName("Gateway");
        config.setBaseUrl("http://localhost:8080");
        config.setRateLimit(0);

        assertThatThrownBy(() -> apiGatewayService.save(config))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void save_success() {
        SfApiGatewayConfig config = new SfApiGatewayConfig();
        config.setName("Gateway");
        config.setBaseUrl("http://localhost:8080");
        when(apiGatewayConfigMapper.insert(any())).thenReturn(1);

        boolean result = apiGatewayService.save(config);

        assertThat(result).isTrue();
    }

    // ------------------------------------------------------------------
    // upsertRoute
    // ------------------------------------------------------------------

    @Test
    void upsertRoute_notFound_throwsNotFound() {
        when(apiGatewayConfigMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> apiGatewayService.upsertRoute(1L, "r1", "/api", "http://target", 1))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.INTEGRATION_NOT_FOUND.getCode());
    }

    @Test
    void upsertRoute_nullRouteId_throwsParamError() {
        SfApiGatewayConfig config = new SfApiGatewayConfig();
        config.setId(1L);
        when(apiGatewayConfigMapper.selectById(1L)).thenReturn(config);

        assertThatThrownBy(() -> apiGatewayService.upsertRoute(1L, null, "/api", "http://target", 1))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void upsertRoute_success_addsRoute() {
        SfApiGatewayConfig config = new SfApiGatewayConfig();
        config.setId(1L);
        when(apiGatewayConfigMapper.selectById(1L)).thenReturn(config);

        apiGatewayService.upsertRoute(1L, "r1", "/api", "http://target", 1);

        List<Map<String, Object>> routes = apiGatewayService.listRoutes(1L);
        assertThat(routes).hasSize(1);
        assertThat(routes.get(0).get("routeId")).isEqualTo("r1");
    }

    @Test
    void upsertRoute_duplicateRouteId_replacesExisting() {
        SfApiGatewayConfig config = new SfApiGatewayConfig();
        config.setId(1L);
        when(apiGatewayConfigMapper.selectById(1L)).thenReturn(config);

        apiGatewayService.upsertRoute(1L, "r1", "/api", "http://target1", 1);
        apiGatewayService.upsertRoute(1L, "r1", "/api", "http://target2", 2);

        List<Map<String, Object>> routes = apiGatewayService.listRoutes(1L);
        assertThat(routes).hasSize(1);
        assertThat(routes.get(0).get("targetUrl")).isEqualTo("http://target2");
    }

    // ------------------------------------------------------------------
    // listRoutes
    // ------------------------------------------------------------------

    @Test
    void listRoutes_notFound_throwsNotFound() {
        when(apiGatewayConfigMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> apiGatewayService.listRoutes(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.INTEGRATION_NOT_FOUND.getCode());
    }

    @Test
    void listRoutes_empty_returnsEmptyList() {
        SfApiGatewayConfig config = new SfApiGatewayConfig();
        config.setId(1L);
        when(apiGatewayConfigMapper.selectById(1L)).thenReturn(config);

        List<Map<String, Object>> result = apiGatewayService.listRoutes(1L);

        assertThat(result).isEmpty();
    }

    // ------------------------------------------------------------------
    // deleteRoute
    // ------------------------------------------------------------------

    @Test
    void deleteRoute_notFound_throwsNotFound() {
        when(apiGatewayConfigMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> apiGatewayService.deleteRoute(1L, "r1"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.INTEGRATION_NOT_FOUND.getCode());
    }

    @Test
    void deleteRoute_routeNotExists_throwsNotFound() {
        SfApiGatewayConfig config = new SfApiGatewayConfig();
        config.setId(1L);
        when(apiGatewayConfigMapper.selectById(1L)).thenReturn(config);

        assertThatThrownBy(() -> apiGatewayService.deleteRoute(1L, "r1"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void deleteRoute_success_removesRoute() {
        SfApiGatewayConfig config = new SfApiGatewayConfig();
        config.setId(1L);
        when(apiGatewayConfigMapper.selectById(1L)).thenReturn(config);

        apiGatewayService.upsertRoute(1L, "r1", "/api", "http://target", 1);
        apiGatewayService.deleteRoute(1L, "r1");

        assertThat(apiGatewayService.listRoutes(1L)).isEmpty();
    }

    // ------------------------------------------------------------------
    // updateRateLimit
    // ------------------------------------------------------------------

    @Test
    void updateRateLimit_notFound_throwsNotFound() {
        when(apiGatewayConfigMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> apiGatewayService.updateRateLimit(1L, 100, 200))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.INTEGRATION_NOT_FOUND.getCode());
    }

    @Test
    void updateRateLimit_nullRps_throwsParamError() {
        SfApiGatewayConfig config = new SfApiGatewayConfig();
        config.setId(1L);
        when(apiGatewayConfigMapper.selectById(1L)).thenReturn(config);

        assertThatThrownBy(() -> apiGatewayService.updateRateLimit(1L, null, 200))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void updateRateLimit_success_updatesConfig() {
        SfApiGatewayConfig config = new SfApiGatewayConfig();
        config.setId(1L);
        config.setName("Gateway");
        config.setBaseUrl("http://localhost:8080");
        when(apiGatewayConfigMapper.selectById(1L)).thenReturn(config);
        when(apiGatewayConfigMapper.updateById(any())).thenReturn(1);

        apiGatewayService.updateRateLimit(1L, 50, 100);

        assertThat(config.getRateLimit()).isEqualTo(50);
        verify(apiGatewayConfigMapper).updateById(config);
    }

    // ------------------------------------------------------------------
    // healthCheck
    // ------------------------------------------------------------------

    @Test
    void healthCheck_gatewayNotFound_returnsFalse() {
        when(apiGatewayConfigMapper.selectById(1L)).thenReturn(null);

        boolean result = apiGatewayService.healthCheck(1L);

        assertThat(result).isFalse();
    }

    @Test
    void healthCheck_nullBaseUrl_returnsFalse() {
        SfApiGatewayConfig config = new SfApiGatewayConfig();
        config.setId(1L);
        config.setBaseUrl(null);
        when(apiGatewayConfigMapper.selectById(1L)).thenReturn(config);

        boolean result = apiGatewayService.healthCheck(1L);

        assertThat(result).isFalse();
    }

    @Test
    void healthCheck_success_returnsTrue() {
        SfApiGatewayConfig config = new SfApiGatewayConfig();
        config.setId(1L);
        config.setBaseUrl("http://localhost:8080");
        when(apiGatewayConfigMapper.selectById(1L)).thenReturn(config);
        when(restTemplate.getForObject("http://localhost:8080/actuator/health", String.class))
                .thenReturn("{\"status\":\"UP\"}");

        boolean result = apiGatewayService.healthCheck(1L);

        assertThat(result).isTrue();
    }

    @Test
    void healthCheck_connectionRefused_returnsFalse() {
        SfApiGatewayConfig config = new SfApiGatewayConfig();
        config.setId(1L);
        config.setBaseUrl("http://localhost:8080");
        when(apiGatewayConfigMapper.selectById(1L)).thenReturn(config);
        doThrow(new org.springframework.web.client.ResourceAccessException("Connection refused"))
                .when(restTemplate).getForObject(any(), any());

        boolean result = apiGatewayService.healthCheck(1L);

        assertThat(result).isFalse();
    }
}
