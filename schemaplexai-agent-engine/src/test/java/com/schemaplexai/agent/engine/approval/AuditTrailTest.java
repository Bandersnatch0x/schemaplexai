package com.schemaplexai.agent.engine.approval;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AuditTrailTest {

    private AuditTrail auditTrail;

    @BeforeEach
    void setUp() {
        auditTrail = new AuditTrail();
    }

    @Test
    void recordShouldStoreEntryAndLogIt() {
        AuditEntry entry = new AuditEntry(
                "exec-1", "system", "APPROVAL_REQUESTED", "Deploy to production", Instant.now()
        );

        auditTrail.record(entry);

        List<AuditEntry> history = auditTrail.getHistory("exec-1");
        assertThat(history).hasSize(1);
        assertThat(history.getFirst().action()).isEqualTo("APPROVAL_REQUESTED");
        assertThat(history.getFirst().actor()).isEqualTo("system");
    }

    @Test
    void getHistoryShouldReturnEmptyListForUnknownExecution() {
        List<AuditEntry> history = auditTrail.getHistory("nonexistent");
        assertThat(history).isEmpty();
    }

    @Test
    void getHistoryShouldReturnEntriesInChronologicalOrder() {
        Instant t1 = Instant.parse("2026-01-01T10:00:00Z");
        Instant t2 = Instant.parse("2026-01-01T10:05:00Z");
        Instant t3 = Instant.parse("2026-01-01T10:10:00Z");

        auditTrail.record(new AuditEntry("exec-1", "system", "APPROVAL_REQUESTED", "desc", t1));
        auditTrail.record(new AuditEntry("exec-1", "approver-1", "APPROVED", "ok", t2));
        auditTrail.record(new AuditEntry("exec-1", "system", "RESUMED", "back to THINKING", t3));

        List<AuditEntry> history = auditTrail.getHistory("exec-1");
        assertThat(history).hasSize(3);
        assertThat(history.get(0).timestamp()).isEqualTo(t1);
        assertThat(history.get(1).timestamp()).isEqualTo(t2);
        assertThat(history.get(2).timestamp()).isEqualTo(t3);
    }

    @Test
    void getHistoryShouldIsolateDifferentExecutions() {
        auditTrail.record(new AuditEntry("exec-1", "system", "ACTION_A", "detail-a", Instant.now()));
        auditTrail.record(new AuditEntry("exec-2", "system", "ACTION_B", "detail-b", Instant.now()));

        assertThat(auditTrail.getHistory("exec-1")).hasSize(1);
        assertThat(auditTrail.getHistory("exec-2")).hasSize(1);
        assertThat(auditTrail.getHistory("exec-1").getFirst().action()).isEqualTo("ACTION_A");
        assertThat(auditTrail.getHistory("exec-2").getFirst().action()).isEqualTo("ACTION_B");
    }

    @Test
    void getHistoryShouldReturnImmutableList() {
        auditTrail.record(new AuditEntry("exec-1", "actor", "ACTION", "detail", Instant.now()));

        List<AuditEntry> history = auditTrail.getHistory("exec-1");
        assertThat(history).hasSize(1);

        // Adding to the returned list should not affect the internal state
        // (List.of / List.copyOf are immutable)
        org.junit.jupiter.api.Assertions.assertThrows(
                UnsupportedOperationException.class,
                () -> history.add(new AuditEntry("exec-1", "x", "Y", "z", Instant.now()))
        );
    }
}
