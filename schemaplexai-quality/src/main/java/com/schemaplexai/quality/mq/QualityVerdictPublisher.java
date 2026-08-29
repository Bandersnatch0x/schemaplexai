package com.schemaplexai.quality.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.common.constants.CommonConstants;
import com.schemaplexai.quality.gate.GateDisposition;
import com.schemaplexai.quality.gate.QualityCheckResult;
import com.schemaplexai.quality.gate.QualityReport;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Publishes structured quality gate verdicts back to the execution side.
 *
 * <p>Verdicts travel on {@code sf.exchange} with routing key
 * {@link CommonConstants#RK_QUALITY_VERDICT}; the Agent engine's
 * QualityVerdictConsumer binds a dedicated queue and applies the disposition
 * semantics (spec §1):
 * PASS continue / WARN alert-and-continue / BLOCK pause for manual
 * confirmation (GATE_BLOCKED) / FAIL terminate.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QualityVerdictPublisher {

    /** Notification chain marker for verdict payloads. */
    public static final String EVENT_TYPE_VERDICT = "QUALITY_GATE_VERDICT";

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Publishes the verdict for one evaluation. Never throws: verdict
     * publication must not roll back the (already persisted) evaluation.
     *
     * @return true if the verdict was handed to MQ, false if publishing failed
     */
    public boolean publishVerdict(Long executionId, Long agentId, String tenantId,
                                  String triggerPoint, QualityReport report) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("eventType", EVENT_TYPE_VERDICT);
            payload.put("eventId", UUID.randomUUID().toString());
            payload.put("executionId", executionId);
            payload.put("agentId", agentId);
            payload.put("tenantId", tenantId);
            payload.put("triggerPoint", triggerPoint);
            payload.put("disposition", report.getDisposition() != null
                    ? report.getDisposition().name() : GateDisposition.PASS.name());
            payload.put("allPassed", report.isAllPassed());
            payload.put("failedRules", failedRules(report));
            payload.put("issueCount", countFailed(report));
            payload.put("checkedAt", Instant.now().toString());
            payload.put("timestamp", System.currentTimeMillis());

            String message = objectMapper.writeValueAsString(payload);
            rabbitTemplate.convertAndSend(CommonConstants.EXCHANGE_SCHEMAPLEXAI,
                    CommonConstants.RK_QUALITY_VERDICT, message);
            log.info("Published quality verdict for execution {}: disposition={}",
                    executionId, payload.get("disposition"));
            return true;
        } catch (Exception e) {
            log.error("Failed to publish quality verdict for execution {}: {}",
                    executionId, e.getMessage(), e);
            return false;
        }
    }

    private List<String> failedRules(QualityReport report) {
        List<String> failed = new ArrayList<>();
        if (report == null || report.getResults() == null) {
            return failed;
        }
        for (QualityCheckResult result : report.getResults()) {
            if (result != null && !result.isPassed()) {
                failed.add(result.getRuleName() != null ? result.getRuleName() : result.getMessage());
            }
        }
        return failed;
    }

    private int countFailed(QualityReport report) {
        if (report == null || report.getResults() == null) {
            return 0;
        }
        return (int) report.getResults().stream()
                .filter(r -> r != null && !r.isPassed())
                .count();
    }
}
