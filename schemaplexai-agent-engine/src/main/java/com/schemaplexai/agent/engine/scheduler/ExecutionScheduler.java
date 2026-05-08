package com.schemaplexai.agent.engine.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread-safe execution scheduler using a priority queue.
 *
 * <p>Ordering rules (highest priority first):
 * <ol>
 *   <li>Lower {@link ExecutionPriority#getWeight()} first</li>
 *   <li>Earlier {@code submittedAt} first (FIFO within same priority)</li>
 *   <li>Lower {@code executionId} as tie-breaker</li>
 * </ol>
 */
@Slf4j
@Service
public class ExecutionScheduler {

    private static final Comparator<PrioritizedExecution> EXECUTION_COMPARATOR =
            Comparator.comparingInt((PrioritizedExecution e) -> e.priority().getWeight())
                    .thenComparing(PrioritizedExecution::submittedAt)
                    .thenComparing(PrioritizedExecution::executionId);

    private final PriorityBlockingQueue<PrioritizedExecution> queue;
    private final Map<Long, PrioritizedExecution> index;
    private final ReentrantLock lock;

    public ExecutionScheduler() {
        this.queue = new PriorityBlockingQueue<>(64, EXECUTION_COMPARATOR);
        this.index = new ConcurrentHashMap<>();
        this.lock = new ReentrantLock();
    }

    /**
     * Submits an execution to the scheduler.
     *
     * @param execution the execution to schedule
     * @return true if newly submitted, false if already present
     */
    public boolean submit(PrioritizedExecution execution) {
        if (execution == null) {
            throw new IllegalArgumentException("execution must not be null");
        }

        lock.lock();
        try {
            if (index.containsKey(execution.executionId())) {
                log.warn("Execution {} already scheduled, ignoring duplicate submit", execution.executionId());
                return false;
            }
            queue.add(execution);
            index.put(execution.executionId(), execution);
            log.debug("Submitted execution {} with priority {}", execution.executionId(), execution.priority());
            return true;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Polls the highest-priority execution from the queue.
     *
     * @return the next execution, or empty if queue is empty
     */
    public Optional<PrioritizedExecution> pollNext() {
        lock.lock();
        try {
            PrioritizedExecution next = queue.poll();
            if (next != null) {
                index.remove(next.executionId());
                log.debug("Polled execution {} with priority {}", next.executionId(), next.priority());
            }
            return Optional.ofNullable(next);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Reorders an existing execution with a new priority.
     *
     * @param executionId the execution to reorder
     * @param newPriority the new priority level
     * @return true if reordered, false if not found
     */
    public boolean reorder(Long executionId, ExecutionPriority newPriority) {
        if (executionId == null) {
            throw new IllegalArgumentException("executionId must not be null");
        }
        if (newPriority == null) {
            throw new IllegalArgumentException("newPriority must not be null");
        }

        lock.lock();
        try {
            PrioritizedExecution existing = index.get(executionId);
            if (existing == null) {
                log.warn("Execution {} not found for reorder", executionId);
                return false;
            }

            queue.remove(existing);
            PrioritizedExecution updated = existing.withPriority(newPriority);
            queue.add(updated);
            index.put(executionId, updated);
            log.debug("Reordered execution {} to priority {}", executionId, newPriority);
            return true;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Cancels a scheduled execution.
     *
     * @param executionId the execution to cancel
     * @return true if cancelled, false if not found
     */
    public boolean cancel(Long executionId) {
        if (executionId == null) {
            throw new IllegalArgumentException("executionId must not be null");
        }

        lock.lock();
        try {
            PrioritizedExecution existing = index.remove(executionId);
            if (existing == null) {
                log.warn("Execution {} not found for cancel", executionId);
                return false;
            }
            queue.remove(existing);
            log.debug("Cancelled execution {}", executionId);
            return true;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns the current number of queued executions.
     */
    public int size() {
        lock.lock();
        try {
            return queue.size();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns an immutable snapshot of all queued executions.
     */
    public java.util.List<PrioritizedExecution> snapshot() {
        lock.lock();
        try {
            return java.util.List.copyOf(queue);
        } finally {
            lock.unlock();
        }
    }
}
