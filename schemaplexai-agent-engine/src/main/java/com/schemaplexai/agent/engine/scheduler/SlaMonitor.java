package com.schemaplexai.agent.engine.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Monitors the execution queue for SLA breaches and tracks queue-time metrics.
 */
@Slf4j
@Service
public class SlaMonitor {

    private final ExecutionScheduler scheduler;
    private final Clock clock;
    private final Map<Long, Instant> deadlines;
    private final ReentrantLock lock;
    private final AtomicLong totalQueueTimeMs;
    private final AtomicLong completedCount;

    public SlaMonitor(ExecutionScheduler scheduler) {
        this(scheduler, Clock.systemUTC());
    }

    SlaMonitor(ExecutionScheduler scheduler, Clock clock) {
        this.scheduler = scheduler;
        this.clock = clock;
        this.deadlines = new ConcurrentHashMap<>();
        this.lock = new ReentrantLock();
        this.totalQueueTimeMs = new AtomicLong(0);
        this.completedCount = new AtomicLong(0);
    }

    /**
     * Registers a deadline for an execution.
     *
     * @param executionId the execution identifier
     * @param deadline    the SLA deadline
     */
    public void registerDeadline(Long executionId, Instant deadline) {
        if (executionId == null) {
            throw new IllegalArgumentException("executionId must not be null");
        }
        if (deadline == null) {
            throw new IllegalArgumentException("deadline must not be null");
        }
        deadlines.put(executionId, deadline);
        log.debug("Registered deadline for execution {}: {}", executionId, deadline);
    }

    /**
     * Removes a deadline registration (e.g., after execution starts or is cancelled).
     *
     * @param executionId the execution identifier
     */
    public void removeDeadline(Long executionId) {
        deadlines.remove(executionId);
    }

    /**
     * Checks the current queue for SLA breaches.
     *
     * @return list of detected breach events
     */
    public List<SlaBreachEvent> checkBreaches() {
        Instant now = clock.instant();
        List<SlaBreachEvent> breaches = new ArrayList<>();

        lock.lock();
        try {
            List<PrioritizedExecution> snapshot = scheduler.snapshot();
            for (PrioritizedExecution execution : snapshot) {
                Instant deadline = deadlines.get(execution.executionId());
                if (deadline == null) {
                    continue;
                }

                if (now.isAfter(deadline)) {
                    SlaBreachEvent.BreachType type = execution.deadline()
                            .filter(d -> now.isAfter(d))
                            .map(d -> SlaBreachEvent.BreachType.DEADLINE_MISSED)
                            .orElse(SlaBreachEvent.BreachType.QUEUE_TIMEOUT);

                    breaches.add(new SlaBreachEvent(
                            execution.executionId(),
                            execution.tenantId(),
                            deadline,
                            Optional.empty(),
                            type
                    ));
                    log.warn("SLA breach detected for execution {}: {}", execution.executionId(), type);
                }
            }
        } finally {
            lock.unlock();
        }

        return List.copyOf(breaches);
    }

    /**
     * Scheduled periodic check for SLA breaches.
     */
    @Scheduled(fixedDelayString = "${scheduler.sla.check-interval-ms:30000}")
    public void scheduledCheck() {
        List<SlaBreachEvent> breaches = checkBreaches();
        if (!breaches.isEmpty()) {
            log.warn("SLA check found {} breach(es)", breaches.size());
        }
    }

    /**
     * Records queue completion time for average calculation.
     *
     * @param executionId  the execution that left the queue
     * @param submittedAt  when the execution was submitted
     */
    public void recordQueueCompletion(Long executionId, Instant submittedAt) {
        if (executionId == null || submittedAt == null) {
            return;
        }
        long queueTimeMs = Duration.between(submittedAt, clock.instant()).toMillis();
        totalQueueTimeMs.addAndGet(queueTimeMs);
        completedCount.incrementAndGet();
        deadlines.remove(executionId);
    }

    /**
     * Returns the average queue time in milliseconds.
     *
     * @return average queue time, or 0 if no completions recorded
     */
    public long getAverageQueueTimeMs() {
        long count = completedCount.get();
        if (count == 0) {
            return 0;
        }
        return totalQueueTimeMs.get() / count;
    }

    /**
     * Returns the total number of completed queue measurements.
     */
    public long getCompletedCount() {
        return completedCount.get();
    }
}
