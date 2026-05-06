package com.schemaplexai.admin.service;

import com.schemaplexai.admin.dto.PlatformHealthDTO;
import com.schemaplexai.admin.entity.SfAuditLog;
import com.schemaplexai.admin.mapper.SfAuditLogMapper;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlatformHealthService {

    private final SfAuditLogMapper auditLogMapper;

    @Value("${spring.application.name:unknown}")
    private String applicationName;

    @Value("${server.port:0}")
    private int serverPort;

    public PlatformHealthDTO checkPlatformHealth() {
        PlatformHealthDTO health = new PlatformHealthDTO();
        health.setCheckedAt(LocalDateTime.now());
        health.setVersion(getVersion());
        health.setUptimeMillis(getUptimeMillis());

        List<PlatformHealthDTO.ServiceHealth> services = checkServices();
        health.setServices(services);

        boolean allHealthy = services.stream()
                .allMatch(s -> "UP".equals(s.getStatus()));
        health.setOverallStatus(allHealthy ? "HEALTHY" : "DEGRADED");

        health.setMetrics(collectJvmMetrics());
        health.setAuditSummary(collectAuditSummary());

        return health;
    }

    public Map<String, Object> collectJvmMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryMXBean.getNonHeapMemoryUsage();

        metrics.put("heapUsedMb", heapUsage.getUsed() / (1024 * 1024));
        metrics.put("heapCommittedMb", heapUsage.getCommitted() / (1024 * 1024));
        metrics.put("heapMaxMb", heapUsage.getMax() / (1024 * 1024));
        metrics.put("nonHeapUsedMb", nonHeapUsage.getUsed() / (1024 * 1024));

        Runtime runtime = Runtime.getRuntime();
        metrics.put("availableProcessors", runtime.availableProcessors());
        metrics.put("freeMemoryMb", runtime.freeMemory() / (1024 * 1024));
        metrics.put("totalMemoryMb", runtime.totalMemory() / (1024 * 1024));

        return metrics;
    }

    public Map<String, Long> collectAuditSummary() {
        Map<String, Long> summary = new HashMap<>();
        LocalDateTime since24h = LocalDateTime.now().minusHours(24);
        LocalDateTime since7d = LocalDateTime.now().minusDays(7);

        Long total24h = auditLogMapper.countByActionSince("ALL", since24h);
        if (total24h == null) {
            total24h = 0L;
        }
        summary.put("totalLast24h", total24h);

        List<SfAuditLogMapper.ActionCount> actionCounts = auditLogMapper.countByActionsSince(since7d);
        if (actionCounts != null) {
            for (SfAuditLogMapper.ActionCount ac : actionCounts) {
                summary.put(ac.action() + "Last7d", ac.count());
            }
        }

        return summary;
    }

    private List<PlatformHealthDTO.ServiceHealth> checkServices() {
        List<PlatformHealthDTO.ServiceHealth> services = new ArrayList<>();

        services.add(checkSelfHealth());

        String[] knownServices = {"schemaplexai-system:8081", "schemaplexai-web:8082",
                "schemaplexai-agent-engine:8084", "schemaplexai-gateway:8080"};

        for (String svc : knownServices) {
            services.add(checkRemoteService(svc));
        }

        return services;
    }

    private PlatformHealthDTO.ServiceHealth checkSelfHealth() {
        PlatformHealthDTO.ServiceHealth health = new PlatformHealthDTO.ServiceHealth();
        health.setName(applicationName);
        health.setStatus("UP");
        health.setEndpoint("http://localhost:" + serverPort);
        health.setResponseTimeMs(0L);
        return health;
    }

    private PlatformHealthDTO.ServiceHealth checkRemoteService(String serviceSpec) {
        String[] parts = serviceSpec.split(":");
        String name = parts[0];
        int port = Integer.parseInt(parts[1]);

        PlatformHealthDTO.ServiceHealth health = new PlatformHealthDTO.ServiceHealth();
        health.setName(name);
        health.setEndpoint("http://localhost:" + port);

        long start = System.currentTimeMillis();
        try (java.net.Socket socket = new java.net.Socket()) {
            socket.connect(new java.net.InetSocketAddress("localhost", port), 2000);
            health.setStatus("UP");
            health.setResponseTimeMs(System.currentTimeMillis() - start);
        } catch (Exception e) {
            health.setStatus("DOWN");
            health.setError(e.getMessage());
            health.setResponseTimeMs(System.currentTimeMillis() - start);
            log.warn("Service {} appears down: {}", name, e.getMessage());
        }

        return health;
    }

    private long getUptimeMillis() {
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        return runtimeMXBean.getUptime();
    }

    private String getVersion() {
        Package pkg = getClass().getPackage();
        return pkg != null && pkg.getImplementationVersion() != null
                ? pkg.getImplementationVersion()
                : "1.0.0-SNAPSHOT";
    }
}
