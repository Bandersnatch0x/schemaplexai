package com.schemaplexai.agent.engine.scheduler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ExecutionScheduler")
class ExecutionSchedulerTest {

    private ExecutionScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new ExecutionScheduler();
    }

    private PrioritizedExecution exec(Long id, ExecutionPriority priority, Instant submittedAt) {
        return new PrioritizedExecution(
                id, 1L, "tenant-1", priority, submittedAt, Optional.empty(), 100L
        );
    }

    @Nested
    @DisplayName("submit")
    class SubmitTests {

        @Test
        @DisplayName("should accept valid execution")
        void shouldAcceptValidExecution() {
            PrioritizedExecution e = exec(1L, ExecutionPriority.NORMAL, Instant.now());

            boolean result = scheduler.submit(e);

            assertTrue(result);
            assertThat(scheduler.size()).isEqualTo(1);
        }

        @Test
        @DisplayName("should reject duplicate executionId")
        void shouldRejectDuplicate() {
            Instant now = Instant.now();
            PrioritizedExecution e1 = exec(1L, ExecutionPriority.NORMAL, now);
            PrioritizedExecution e2 = exec(1L, ExecutionPriority.HIGH, now.plusMillis(1));

            assertTrue(scheduler.submit(e1));
            assertFalse(scheduler.submit(e2));
            assertThat(scheduler.size()).isEqualTo(1);
        }

        @Test
        @DisplayName("should throw on null execution")
        void shouldThrowOnNull() {
            assertThrows(IllegalArgumentException.class, () -> scheduler.submit(null));
        }
    }

    @Nested
    @DisplayName("pollNext")
    class PollNextTests {

        @Test
        @DisplayName("should return empty when queue is empty")
        void shouldReturnEmpty() {
            Optional<PrioritizedExecution> result = scheduler.pollNext();

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should poll in priority order")
        void shouldPollInPriorityOrder() {
            Instant now = Instant.now();
            scheduler.submit(exec(1L, ExecutionPriority.LOW, now));
            scheduler.submit(exec(2L, ExecutionPriority.CRITICAL, now));
            scheduler.submit(exec(3L, ExecutionPriority.HIGH, now));
            scheduler.submit(exec(4L, ExecutionPriority.NORMAL, now));

            assertThat(scheduler.pollNext().map(PrioritizedExecution::executionId)).hasValue(2L);
            assertThat(scheduler.pollNext().map(PrioritizedExecution::executionId)).hasValue(3L);
            assertThat(scheduler.pollNext().map(PrioritizedExecution::executionId)).hasValue(4L);
            assertThat(scheduler.pollNext().map(PrioritizedExecution::executionId)).hasValue(1L);
            assertTrue(scheduler.pollNext().isEmpty());
        }

        @Test
        @DisplayName("should use FIFO within same priority")
        void shouldUseFifoWithinSamePriority() {
            Instant now = Instant.now();
            scheduler.submit(exec(1L, ExecutionPriority.NORMAL, now));
            scheduler.submit(exec(2L, ExecutionPriority.NORMAL, now.plusMillis(10)));
            scheduler.submit(exec(3L, ExecutionPriority.NORMAL, now.plusMillis(5)));

            assertThat(scheduler.pollNext().map(PrioritizedExecution::executionId)).hasValue(1L);
            assertThat(scheduler.pollNext().map(PrioritizedExecution::executionId)).hasValue(3L);
            assertThat(scheduler.pollNext().map(PrioritizedExecution::executionId)).hasValue(2L);
        }

        @Test
        @DisplayName("should decrease size after poll")
        void shouldDecreaseSize() {
            scheduler.submit(exec(1L, ExecutionPriority.NORMAL, Instant.now()));

            scheduler.pollNext();

            assertThat(scheduler.size()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("reorder")
    class ReorderTests {

        @Test
        @DisplayName("should reorder existing execution")
        void shouldReorderExisting() {
            Instant now = Instant.now();
            scheduler.submit(exec(1L, ExecutionPriority.LOW, now));
            scheduler.submit(exec(2L, ExecutionPriority.HIGH, now));

            boolean reordered = scheduler.reorder(1L, ExecutionPriority.CRITICAL);

            assertTrue(reordered);
            assertThat(scheduler.pollNext().map(PrioritizedExecution::executionId)).hasValue(1L);
            assertThat(scheduler.pollNext().map(PrioritizedExecution::executionId)).hasValue(2L);
        }

        @Test
        @DisplayName("should return false for unknown execution")
        void shouldReturnFalseForUnknown() {
            boolean reordered = scheduler.reorder(99L, ExecutionPriority.CRITICAL);

            assertFalse(reordered);
        }

        @Test
        @DisplayName("should throw on null executionId")
        void shouldThrowOnNullId() {
            assertThrows(IllegalArgumentException.class, () -> scheduler.reorder(null, ExecutionPriority.HIGH));
        }

        @Test
        @DisplayName("should throw on null priority")
        void shouldThrowOnNullPriority() {
            assertThrows(IllegalArgumentException.class, () -> scheduler.reorder(1L, null));
        }
    }

    @Nested
    @DisplayName("cancel")
    class CancelTests {

        @Test
        @DisplayName("should cancel existing execution")
        void shouldCancelExisting() {
            scheduler.submit(exec(1L, ExecutionPriority.NORMAL, Instant.now()));

            boolean cancelled = scheduler.cancel(1L);

            assertTrue(cancelled);
            assertThat(scheduler.size()).isEqualTo(0);
        }

        @Test
        @DisplayName("should return false for unknown execution")
        void shouldReturnFalseForUnknown() {
            boolean cancelled = scheduler.cancel(99L);

            assertFalse(cancelled);
        }

        @Test
        @DisplayName("should throw on null executionId")
        void shouldThrowOnNullId() {
            assertThrows(IllegalArgumentException.class, () -> scheduler.cancel(null));
        }
    }

    @Nested
    @DisplayName("thread safety")
    class ThreadSafetyTests {

        @Test
        @DisplayName("should handle concurrent submits safely")
        void shouldHandleConcurrentSubmits() throws InterruptedException {
            int threadCount = 20;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);

            for (int i = 0; i < threadCount; i++) {
                final long id = i + 1;
                executor.submit(() -> {
                    try {
                        scheduler.submit(exec(id, ExecutionPriority.NORMAL, Instant.now()));
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            executor.shutdown();

            assertThat(scheduler.size()).isEqualTo(threadCount);
        }

        @Test
        @DisplayName("should handle concurrent submit and poll safely")
        void shouldHandleConcurrentSubmitAndPoll() throws InterruptedException {
            int submitCount = 100;
            int pollThreadCount = 10;
            ExecutorService submitExecutor = Executors.newFixedThreadPool(10);
            ExecutorService pollExecutor = Executors.newFixedThreadPool(pollThreadCount);
            CountDownLatch submitLatch = new CountDownLatch(submitCount);
            CountDownLatch pollLatch = new CountDownLatch(pollThreadCount);
            AtomicInteger polledCount = new AtomicInteger(0);

            for (int i = 0; i < submitCount; i++) {
                final long id = i + 1;
                submitExecutor.submit(() -> {
                    try {
                        scheduler.submit(exec(id, ExecutionPriority.NORMAL, Instant.now()));
                    } finally {
                        submitLatch.countDown();
                    }
                });
            }

            submitLatch.await();

            for (int i = 0; i < pollThreadCount; i++) {
                pollExecutor.submit(() -> {
                    try {
                        while (scheduler.pollNext().isPresent()) {
                            polledCount.incrementAndGet();
                        }
                    } finally {
                        pollLatch.countDown();
                    }
                });
            }

            pollLatch.await();
            pollExecutor.shutdown();
            submitExecutor.shutdown();

            assertThat(polledCount.get()).isEqualTo(submitCount);
            assertThat(scheduler.size()).isEqualTo(0);
        }

        @Test
        @DisplayName("should handle concurrent reorder operations safely")
        void shouldHandleConcurrentReorders() throws InterruptedException {
            int count = 50;
            for (int i = 0; i < count; i++) {
                scheduler.submit(exec((long) i, ExecutionPriority.NORMAL, Instant.now()));
            }

            ExecutorService executor = Executors.newFixedThreadPool(10);
            CountDownLatch latch = new CountDownLatch(count);

            for (int i = 0; i < count; i++) {
                final long id = i;
                ExecutionPriority newPriority = (i % 2 == 0) ? ExecutionPriority.HIGH : ExecutionPriority.LOW;
                executor.submit(() -> {
                    try {
                        scheduler.reorder(id, newPriority);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            executor.shutdown();

            assertThat(scheduler.size()).isEqualTo(count);

            List<PrioritizedExecution> polled = new ArrayList<>();
            Optional<PrioritizedExecution> next;
            while ((next = scheduler.pollNext()).isPresent()) {
                polled.add(next.get());
            }

            // All HIGH priorities should come before LOW
            boolean seenLow = false;
            for (PrioritizedExecution e : polled) {
                if (e.priority() == ExecutionPriority.LOW) {
                    seenLow = true;
                }
                if (e.priority() == ExecutionPriority.HIGH) {
                    assertFalse(seenLow, "HIGH should not appear after LOW");
                }
            }
        }
    }

    @Nested
    @DisplayName("snapshot")
    class SnapshotTests {

        @Test
        @DisplayName("should return immutable snapshot")
        void shouldReturnImmutableSnapshot() {
            scheduler.submit(exec(1L, ExecutionPriority.NORMAL, Instant.now()));

            List<PrioritizedExecution> snapshot = scheduler.snapshot();

            assertThat(snapshot).hasSize(1);
            assertThrows(UnsupportedOperationException.class, () -> snapshot.add(exec(2L, ExecutionPriority.HIGH, Instant.now())));
        }
    }
}
