package com.schemaplexai.admin.controller;

import com.schemaplexai.admin.dto.PlatformHealthDTO;
import com.schemaplexai.admin.service.PlatformHealthService;
import com.schemaplexai.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@Tag(name = "平台健康监控")
@RestController
@RequestMapping("/admin/health")
@RequiredArgsConstructor
public class HealthController extends BaseAdminController {

    private final PlatformHealthService platformHealthService;

    @Operation(summary = "平台整体健康检查")
    @GetMapping
    public Result<PlatformHealthDTO> health() {
        PlatformHealthDTO health = platformHealthService.checkPlatformHealth();
        return success(health);
    }

    @Operation(summary = "JVM 运行时指标")
    @GetMapping("/metrics")
    public Result<Map<String, Object>> metrics() {
        return success(platformHealthService.collectJvmMetrics());
    }

    @Operation(summary = "审计日志汇总")
    @GetMapping("/audit-summary")
    public Result<Map<String, Long>> auditSummary() {
        return success(platformHealthService.collectAuditSummary());
    }
}
