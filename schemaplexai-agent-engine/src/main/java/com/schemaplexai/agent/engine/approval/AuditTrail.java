package com.schemaplexai.agent.engine.approval;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe in-memory audit trail for HITL approval actions.
 * <p>
 * Each entry is both persisted in memory (for programmatic retrieval) and
 * logged via SLF4J (for external log aggregation).
 * <p>
 * v1 implementation — entries are not persisted to a database.
 */
@Slf4j
@Component
public class AuditTrail {

    private final Map<String, List<AuditEntry>> entriesByExecution = new ConcurrentHashMap<>();

    /**
     * Records an audit entry: stores in memory and logs via SLF4J.
     *
     * @param entry the audit entry to record
     */
    public void record(AuditEntry entry) {
        entriesByExecution
                .computeIfAbsent(entry.executionId(), k -> new CopyOnWriteArrayList<>())
                .add(entry);

        log.info("AUDIT [execution={}] actor={} action={} detail={}",
                entry.executionId(), entry.actor(), entry.action(), entry.detail());
    }

    /**
     * Returns the full audit history for a given execution, in chronological order.
     *
     * @param executionId the execution to look up
     * @return list of audit entries, empty if none recorded
     */
    public List<AuditEntry> getHistory(String executionId) {
        List<AuditEntry> entries = entriesByExecution.get(executionId);
        if (entries == null) {
            return List.of();
        }
        return List.copyOf(entries);
    }
}
