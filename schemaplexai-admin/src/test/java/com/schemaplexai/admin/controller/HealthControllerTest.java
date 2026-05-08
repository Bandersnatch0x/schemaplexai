package com.schemaplexai.admin.controller;

import com.schemaplexai.admin.dto.PlatformHealthDTO;
import com.schemaplexai.admin.service.PlatformHealthService;
import com.schemaplexai.common.result.Result;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HealthControllerTest {

    @Mock
    private PlatformHealthService platformHealthService;

    @InjectMocks
    private HealthController healthController;

    @Test
    void health_returnsHealthDto() {
        PlatformHealthDTO health = new PlatformHealthDTO();
        health.setOverallStatus("HEALTHY");
        when(platformHealthService.checkPlatformHealth()).thenReturn(health);

        Result<PlatformHealthDTO> result = healthController.health();

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getOverallStatus()).isEqualTo("HEALTHY");
    }

    @Test
    void metrics_returnsJvmMetrics() {
        Map<String, Object> metrics = Map.of("heapUsedMb", 128L);
        when(platformHealthService.collectJvmMetrics()).thenReturn(metrics);

        Result<Map<String, Object>> result = healthController.metrics();

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().get("heapUsedMb")).isEqualTo(128L);
    }

    @Test
    void auditSummary_returnsSummary() {
        Map<String, Long> summary = Map.of("totalLast24h", 42L);
        when(platformHealthService.collectAuditSummary()).thenReturn(summary);

        Result<Map<String, Long>> result = healthController.auditSummary();

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().get("totalLast24h")).isEqualTo(42L);
    }
}
