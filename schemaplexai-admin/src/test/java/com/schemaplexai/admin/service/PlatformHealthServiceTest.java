package com.schemaplexai.admin.service;

import com.schemaplexai.admin.dto.PlatformHealthDTO;
import com.schemaplexai.admin.mapper.SfAuditLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlatformHealthServiceTest {

    @Mock
    private SfAuditLogMapper auditLogMapper;

    @InjectMocks
    private PlatformHealthService platformHealthService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(platformHealthService, "applicationName", "schemaplexai-admin");
        ReflectionTestUtils.setField(platformHealthService, "serverPort", 8081);
    }

    // ------------------------------------------------------------------
    // checkPlatformHealth
    // ------------------------------------------------------------------

    @Test
    void checkPlatformHealth_returnsDto() {
        when(auditLogMapper.countByActionSince(any(), any())).thenReturn(10L);
        when(auditLogMapper.countByActionsSince(any())).thenReturn(Collections.emptyList());

        PlatformHealthDTO result = platformHealthService.checkPlatformHealth();

        assertThat(result.getCheckedAt()).isNotNull();
        assertThat(result.getVersion()).isNotNull();
        assertThat(result.getOverallStatus()).isIn("HEALTHY", "DEGRADED");
        assertThat(result.getServices()).isNotEmpty();
        assertThat(result.getMetrics()).isNotEmpty();
        assertThat(result.getAuditSummary()).isNotNull();
    }

    @Test
    void checkPlatformHealth_allServicesUp_returnsHealthy() {
        when(auditLogMapper.countByActionSince(any(), any())).thenReturn(10L);
        when(auditLogMapper.countByActionsSince(any())).thenReturn(Collections.emptyList());

        PlatformHealthDTO result = platformHealthService.checkPlatformHealth();

        assertThat(result.getServices()).allMatch(s -> s.getName() != null);
        assertThat(result.getServices().get(0).getName()).isEqualTo("schemaplexai-admin");
        assertThat(result.getServices().get(0).getStatus()).isEqualTo("UP");
    }

    // ------------------------------------------------------------------
    // collectJvmMetrics
    // ------------------------------------------------------------------

    @Test
    void collectJvmMetrics_returnsExpectedKeys() {
        Map<String, Object> result = platformHealthService.collectJvmMetrics();

        assertThat(result).containsKeys("heapUsedMb", "heapCommittedMb", "heapMaxMb",
                "nonHeapUsedMb", "availableProcessors", "freeMemoryMb", "totalMemoryMb");
        assertThat((Long) result.get("heapUsedMb")).isGreaterThanOrEqualTo(0);
        assertThat((Integer) result.get("availableProcessors")).isGreaterThan(0);
    }

    // ------------------------------------------------------------------
    // collectAuditSummary
    // ------------------------------------------------------------------

    @Test
    void collectAuditSummary_noData_returnsZeros() {
        when(auditLogMapper.countByActionSince(eq("ALL"), any())).thenReturn(0L);
        when(auditLogMapper.countByActionsSince(any())).thenReturn(Collections.emptyList());

        Map<String, Long> result = platformHealthService.collectAuditSummary();

        assertThat(result.get("totalLast24h")).isEqualTo(0);
    }

    @Test
    void collectAuditSummary_withData_returnsCounts() {
        when(auditLogMapper.countByActionSince(eq("ALL"), any())).thenReturn(42L);
        when(auditLogMapper.countByActionsSince(any())).thenReturn(List.of(
                new SfAuditLogMapper.ActionCount("LOGIN", 5L),
                new SfAuditLogMapper.ActionCount("LOGOUT", 3L)
        ));

        Map<String, Long> result = platformHealthService.collectAuditSummary();

        assertThat(result.get("totalLast24h")).isEqualTo(42);
        assertThat(result.get("LOGINLast7d")).isEqualTo(5);
        assertThat(result.get("LOGOUTLast7d")).isEqualTo(3);
    }

    @Test
    void collectAuditSummary_nullCount_returnsZero() {
        when(auditLogMapper.countByActionSince(eq("ALL"), any())).thenReturn(null);
        when(auditLogMapper.countByActionsSince(any())).thenReturn(null);

        Map<String, Long> result = platformHealthService.collectAuditSummary();

        assertThat(result.get("totalLast24h")).isEqualTo(0);
    }
}
