package com.schemaplexai.agent.engine.scheduler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SlaMonitor")
class SlaMonitorTest {

    private ExecutionScheduler scheduler;
    private Clock fixedClock;
    private SlaMonitor monitor;

    @BeforeEach
    void setUp() {
        scheduler = new ExecutionScheduler();
        fixedClock = Clock.fixed(Instant.parse("2024-01-01T12:00:00Z"), ZoneId.of("UTC"));
        monitor = new SlaMonitor(scheduler, fixedClock);
    }

    private PrioritizedExecution exec(Long id, Instant submittedAt, Optional<Instant> deadline) {
        return new PrioritizedExecution(
                id, 1L, "tenant-1", ExecutionPriority.NORMAL, submittedAt, deadline, 100L
        );
    }

    @Nested
    @DisplayName("registerDeadline")
    class RegisterDeadlineTests {

        @Test
        @DisplayName("should register deadline successfully")
        void shouldRegisterDeadline() {
            Instant deadline = Instant.parse("2024-01-01T12:05:00Z");

            monitor.registerDeadline(1L, deadline);

            List<SlaBreachEvent> breaches = monitor.checkBreaches();
            assertThat(breaches).isEmpty();
        }

        @Test
        @DisplayName("should throw on null executionId")
        void shouldThrowOnNullId() {
            assertThrows(IllegalArgumentException.class, () -> monitor.registerDeadline(null, Instant.now()));
        }

        @Test
        @DisplayName("should throw on null deadline")
        void shouldThrowOnNullDeadline() {
            assertThrows(IllegalArgumentException.class, () -> monitor.registerDeadline(1L, null));
        }
    }

    @Nested
    @DisplayName("checkBreaches")
    class CheckBreachesTests {

        @Test
        @DisplayName("should detect DEADLINE_MISSED when execution deadline passed")
        void shouldDetectDeadlineMissed() {
            Instant submittedAt = Instant.parse("2024-01-01T11:50:00Z");
            Instant deadline = Instant.parse("2024-01-01T11:55:00Z");
            scheduler.submit(exec(1L, submittedAt, Optional.of(deadline)));
            monitor.registerDeadline(1L, deadline);

            List<SlaBreachEvent> breaches = monitor.checkBreaches();

            assertThat(breaches).hasSize(1);
            SlaBreachEvent breach = breaches.get(0);
            assertThat(breach.executionId()).isEqualTo(1L);
            assertThat(breach.breachType()).isEqualTo(SlaBreachEvent.BreachType.DEADLINE_MISSED);
            assertThat(breach.tenantId()).isEqualTo("tenant-1");
            assertTrue(breach.actualStartTime().isEmpty());
        }

        @Test
        @DisplayName("should detect QUEUE_TIMEOUT when registered deadline passed without execution deadline")
        void shouldDetectQueueTimeout() {
            Instant submittedAt = Instant.parse("2024-01-01T11:50:00Z");
            scheduler.submit(exec(1L, submittedAt, Optional.empty()));
            monitor.registerDeadline(1L, Instant.parse("2024-01-01T11:55:00Z"));

            List<SlaBreachEvent> breaches = monitor.checkBreaches();

            assertThat(breaches).hasSize(1);
            assertThat(breaches.get(0).breachType()).isEqualTo(SlaBreachEvent.BreachType.QUEUE_TIMEOUT);
        }

        @Test
        @DisplayName("should return empty when no deadlines are breached")
        void shouldReturnEmptyWhenNoBreach() {
            Instant submittedAt = Instant.parse("2024-01-01T11:50:00Z");
            Instant deadline = Instant.parse("2024-01-01T12:10:00Z");
            scheduler.submit(exec(1L, submittedAt, Optional.of(deadline)));
            monitor.registerDeadline(1L, deadline);

            List<SlaBreachEvent> breaches = monitor.checkBreaches();

            assertThat(breaches).isEmpty();
        }

        @Test
        @DisplayName("should return empty when no deadlines registered")
        void shouldReturnEmptyWhenNoDeadlines() {
            scheduler.submit(exec(1L, Instant.now(), Optional.empty()));

            List<SlaBreachEvent> breaches = monitor.checkBreaches();

            assertThat(breaches).isEmpty();
        }

        @Test
        @DisplayName("should detect multiple breaches")
        void shouldDetectMultipleBreaches() {
            Instant submittedAt = Instant.parse("2024-01-01T11:50:00Z");
            scheduler.submit(exec(1L, submittedAt, Optional.of(Instant.parse("2024-01-01T11:55:00Z"))));
            scheduler.submit(exec(2L, submittedAt, Optional.of(Instant.parse("2024-01-01T11:56:00Z"))));
            monitor.registerDeadline(1L, Instant.parse("2024-01-01T11:55:00Z"));
            monitor.registerDeadline(2L, Instant.parse("2024-01-01T11:56:00Z"));

            List<SlaBreachEvent> breaches = monitor.checkBreaches();

            assertThat(breaches).hasSize(2);
        }
    }

    @Nested
    @DisplayName("recordQueueCompletion")
    class RecordQueueCompletionTests {

        @Test
        @DisplayName("should record queue completion and update average")
        void shouldRecordQueueCompletion() {
            Instant submittedAt = Instant.parse("2024-01-01T11:59:00Z");
            monitor.recordQueueCompletion(1L, submittedAt);

            assertThat(monitor.getAverageQueueTimeMs()).isEqualTo(60_000L);
            assertThat(monitor.getCompletedCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("should calculate average across multiple completions")
        void shouldCalculateAverage() {
            monitor.recordQueueCompletion(1L, Instant.parse("2024-01-01T11:59:00Z")); // 60s
            monitor.recordQueueCompletion(2L, Instant.parse("2024-01-01T11:59:30Z")); // 30s

            assertThat(monitor.getAverageQueueTimeMs()).isEqualTo(45_000L);
            assertThat(monitor.getCompletedCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("should remove deadline on queue completion")
        void shouldRemoveDeadlineOnCompletion() {
            Instant deadline = Instant.parse("2024-01-01T11:55:00Z");
            scheduler.submit(exec(1L, Instant.parse("2024-01-01T11:50:00Z"), Optional.of(deadline)));
            monitor.registerDeadline(1L, deadline);

            monitor.recordQueueCompletion(1L, Instant.parse("2024-01-01T11:58:00Z"));

            // After completion, the deadline should be removed so no breach
            List<SlaBreachEvent> breaches = monitor.checkBreaches();
            assertThat(breaches).isEmpty();
        }

        @Test
        @DisplayName("should ignore null inputs")
        void shouldIgnoreNullInputs() {
            monitor.recordQueueCompletion(null, Instant.now());
            monitor.recordQueueCompletion(1L, null);

            assertThat(monitor.getAverageQueueTimeMs()).isEqualTo(0);
            assertThat(monitor.getCompletedCount()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("getAverageQueueTimeMs")
    class AverageQueueTimeTests {

        @Test
        @DisplayName("should return 0 when no completions")
        void shouldReturnZeroWhenEmpty() {
            assertThat(monitor.getAverageQueueTimeMs()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("removeDeadline")
    class RemoveDeadlineTests {

        @Test
        @DisplayName("should remove registered deadline")
        void shouldRemoveDeadline() {
            Instant deadline = Instant.parse("2024-01-01T11:55:00Z");
            scheduler.submit(exec(1L, Instant.parse("2024-01-01T11:50:00Z"), Optional.of(deadline)));
            monitor.registerDeadline(1L, deadline);

            monitor.removeDeadline(1L);

            List<SlaBreachEvent> breaches = monitor.checkBreaches();
            assertThat(breaches).isEmpty();
        }
    }
}
