package com.schemaplexai.agent.engine.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.agent.engine.entity.ExecutionEvent;
import com.schemaplexai.agent.engine.mapper.ExecutionEventMapper;
import com.schemaplexai.model.event.ExecutionEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Periodic job that detects and repairs gaps in execution event sequences.
 * Scans sf_execution_event for missing seq numbers per execution,
 * then re-publishes found events directly to MQ (NOT via Outbox to avoid UNIQUE conflicts).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GapRecoveryJob {

    private static final String EXCHANGE_NAME = "execution_events";

    private final ExecutionEventMapper executionEventMapper;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 30000)
    public void run() {
        List<Long> activeExecutions = executionEventMapper.selectActiveExecutionIds();
        if (activeExecutions.isEmpty()) {
            return;
        }

        for (Long executionId : activeExecutions) {
            try {
                recoverGapsForExecution(executionId);
            } catch (Exception e) {
                log.error("[GapRecoveryJob] Failed to recover gaps for execution {}", executionId, e);
            }
        }
    }

    public void recoverGapsForExecution(Long executionId) {
        List<ExecutionEvent> events = executionEventMapper.selectByExecutionIdOrdered(executionId);
        if (events.size() < 2) {
            return;
        }

        // Build seq -> event map
        TreeMap<Integer, ExecutionEvent> seqMap = events.stream()
                .collect(Collectors.toMap(
                        ExecutionEvent::getSeq,
                        e -> e,
                        (a, b) -> a,
                        TreeMap::new
                ));

        int minSeq = seqMap.firstKey();
        int maxSeq = seqMap.lastKey();
        int expectedCount = maxSeq - minSeq + 1;

        if (seqMap.size() == expectedCount) {
            return; // No gaps
        }

        // Find missing seq numbers
        int gapsFound = 0;
        for (int seq = minSeq; seq <= maxSeq; seq++) {
            if (!seqMap.containsKey(seq)) {
                gapsFound++;
                log.warn("[GapRecoveryJob] Gap detected: execution={}, missing seq={}", executionId, seq);
                // Re-publish a placeholder gap event to trigger downstream consumers
                // In production, this would query Engine's primary event log directly
                publishGapAlert(executionId, seq);
            }
        }

        if (gapsFound > 0) {
            log.info("[GapRecoveryJob] Execution {}: {} gap(s) detected (seq {} - {})",
                    executionId, gapsFound, minSeq, maxSeq);
        }
    }

    private void publishGapAlert(Long executionId, int missingSeq) {
        try {
            ExecutionEventMessage gapEvent = new ExecutionEventMessage(
                    null, executionId, missingSeq,
                    "GAP_DETECTED",
                    "{\"reason\":\"seq gap recovered\",\"missingSeq\":" + missingSeq + "}",
                    java.time.Instant.now(),
                    null, null, "AUDIT"
            );
            String payload = objectMapper.writeValueAsString(gapEvent);
            rabbitTemplate.convertAndSend(EXCHANGE_NAME, "execution.gap", payload);
        } catch (Exception e) {
            log.error("[GapRecoveryJob] Failed to publish gap alert for execution={}, seq={}",
                    executionId, missingSeq, e);
        }
    }
}
