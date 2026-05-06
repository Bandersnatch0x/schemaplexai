package com.schemaplexai.admin.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class PlatformHealthDTO {

    private String overallStatus;
    private LocalDateTime checkedAt;
    private long uptimeMillis;
    private String version;

    private List<ServiceHealth> services;
    private Map<String, Object> metrics;
    private Map<String, Long> auditSummary;

    @Data
    public static class ServiceHealth {
        private String name;
        private String status;
        private String endpoint;
        private Long responseTimeMs;
        private String error;
    }
}
