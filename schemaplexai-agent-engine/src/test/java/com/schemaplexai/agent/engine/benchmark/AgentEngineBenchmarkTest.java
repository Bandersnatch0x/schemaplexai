package com.schemaplexai.agent.engine.benchmark;

import com.schemaplexai.agent.engine.admission.TokenBudget;
import com.schemaplexai.agent.engine.loop.AgentLoopDetectionService;
import com.schemaplexai.agent.engine.model.LlmMessage;
import com.schemaplexai.agent.engine.tool.InMemoryToolRegistry;
import com.schemaplexai.agent.engine.tool.ToolDefinition;
import com.schemaplexai.agent.engine.tool.ToolParameter;
import com.schemaplexai.agent.engine.util.TokenEstimator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Performance and benchmark tests for agent-engine core components.
 * Measures: latency (ms), throughput (ops/sec), token estimation accuracy.
 */
@DisplayName("Agent Engine Performance Benchmarks")
class AgentEngineBenchmarkTest {

    private static final int WARMUP_ITERATIONS = 1000;
    private static final int BENCHMARK_ITERATIONS = 100_000;
    private static final int CONCURRENT_THREADS = 16;

    private TokenBudget tokenBudget;
    private AgentLoopDetectionService loopDetectionService;
    private InMemoryToolRegistry toolRegistry;

    @BeforeEach
    void setUp() {
        tokenBudget = new TokenBudget(1_000_000, 500_000, 1000);
        loopDetectionService = new AgentLoopDetectionService(5, 3, 3);
        toolRegistry = new InMemoryToolRegistry();
        toolRegistry.register(new ToolDefinition("web_search", "Search the web",
                List.of(new ToolParameter("query", "string", "Search query", true)), "string"));
    }

    // ------------------------------------------------------------------
    // TokenBudget benchmarks
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Benchmark: TokenBudget.consumeInput throughput")
    void tokenBudgetConsumeInputThroughput() {
        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            tokenBudget.consumeInput(1);
        }
        tokenBudget = new TokenBudget(1_000_000, 500_000, 1000);

        long start = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            tokenBudget.consumeInput(1);
        }
        long elapsedNs = System.nanoTime() - start;
        double opsPerSec = BENCHMARK_ITERATIONS / (elapsedNs / 1_000_000_000.0);
        double latencyUs = elapsedNs / 1000.0 / BENCHMARK_ITERATIONS;

        System.out.printf("[TokenBudget.consumeInput] %,d ops/sec, %.3f us/op%n", (long) opsPerSec, latencyUs);
        assertThat(opsPerSec).isGreaterThan(1_000_000); // Expect >1M ops/sec
    }

    @Test
    @DisplayName("Benchmark: TokenBudget concurrent consume throughput")
    void tokenBudgetConcurrentThroughput() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        tokenBudget = new TokenBudget(Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE);

        long start = System.nanoTime();
        List<Future<?>> futures = new ArrayList<>();
        int perThread = BENCHMARK_ITERATIONS / CONCURRENT_THREADS;
        for (int t = 0; t < CONCURRENT_THREADS; t++) {
            futures.add(executor.submit(() -> {
                for (int i = 0; i < perThread; i++) {
                    tokenBudget.consumeInput(1);
                }
            }));
        }
        for (Future<?> f : futures) f.get();
        long elapsedNs = System.nanoTime() - start;
        executor.shutdown();

        double opsPerSec = BENCHMARK_ITERATIONS / (elapsedNs / 1_000_000_000.0);
        System.out.printf("[TokenBudget.concurrentConsume] %,d ops/sec (threads=%d)%n", (long) opsPerSec, CONCURRENT_THREADS);
        assertThat(opsPerSec).isGreaterThan(500_000);
    }

    // ------------------------------------------------------------------
    // TokenEstimator benchmarks
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Benchmark: TokenEstimator.estimate latency for typical prompt")
    void tokenEstimatorLatency() {
        String prompt = "Explain the difference between REST and GraphQL APIs in detail. " +
                "Include examples of when to use each, performance considerations, and caching strategies.";

        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            TokenEstimator.estimate(prompt);
        }

        long start = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            TokenEstimator.estimate(prompt);
        }
        long elapsedNs = System.nanoTime() - start;
        double latencyNs = (double) elapsedNs / BENCHMARK_ITERATIONS;

        System.out.printf("[TokenEstimator.estimate] %.1f ns/op%n", latencyNs);
        assertThat(latencyNs).isLessThan(500); // Expect <500ns per estimation
    }

    @Test
    @DisplayName("Benchmark: TokenEstimator throughput for batch messages")
    void tokenEstimatorBatchThroughput() {
        List<LlmMessage> messages = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            messages.add(new LlmMessage("user", "Message content " + i));
        }

        long start = System.nanoTime();
        for (int i = 0; i < 10_000; i++) {
            TokenEstimator.estimate(messages);
        }
        long elapsedNs = System.nanoTime() - start;
        double opsPerSec = 10_000 / (elapsedNs / 1_000_000_000.0);

        System.out.printf("[TokenEstimator.batch] %,d ops/sec (20 messages/batch)%n", (long) opsPerSec);
        assertThat(opsPerSec).isGreaterThan(50_000);
    }

    // ------------------------------------------------------------------
    // ToolRegistry benchmarks
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Benchmark: ToolRegistry get throughput")
    void toolRegistryThroughput() {
        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            toolRegistry.get("web_search");
        }

        long start = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            toolRegistry.get("web_search");
        }
        long elapsedNs = System.nanoTime() - start;
        double opsPerSec = BENCHMARK_ITERATIONS / (elapsedNs / 1_000_000_000.0);
        double latencyNs = (double) elapsedNs / BENCHMARK_ITERATIONS;

        System.out.printf("[ToolRegistry.get] %,d ops/sec, %.1f ns/op%n", (long) opsPerSec, latencyNs);
        assertThat(opsPerSec).isGreaterThan(5_000_000); // Expect >5M lookups/sec
    }

    // ------------------------------------------------------------------
    // AgentLoopDetectionService benchmarks
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Benchmark: Loop detection throughput for repeated tool calls")
    void loopDetectionThroughput() {
        Long execId = 123L;
        String hash = "hash-abc";
        List<String> toolSequence = List.of("search", "read", "summarize");

        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            loopDetectionService.detectLoop(execId, hash, toolSequence);
        }
        loopDetectionService.clearRecords(execId);

        long start = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            loopDetectionService.detectLoop(execId, hash + i, toolSequence);
        }
        long elapsedNs = System.nanoTime() - start;
        double opsPerSec = BENCHMARK_ITERATIONS / (elapsedNs / 1_000_000_000.0);

        System.out.printf("[AgentLoopDetectionService.detectLoop] %,d ops/sec%n", (long) opsPerSec);
        assertThat(opsPerSec).isGreaterThan(30_000);
    }

    // ------------------------------------------------------------------
    // Composite benchmark summary
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Benchmark: Composite agent-engine operations summary")
    void compositeBenchmarkSummary() {
        System.out.println("\n=== Agent Engine Performance Summary ===");

        // TokenBudget
        TokenBudget tb = new TokenBudget(1_000_000, 500_000, 1000);
        long t1 = System.nanoTime();
        for (int i = 0; i < 1_000_000; i++) tb.consumeInput(1);
        long tbOps = (long) (1_000_000 / ((System.nanoTime() - t1) / 1_000_000_000.0));
        System.out.printf("TokenBudget.consumeInput: %,d ops/sec%n", tbOps);

        // TokenEstimator
        String sample = "A typical user prompt with about one hundred characters of text content here.";
        long t2 = System.nanoTime();
        for (int i = 0; i < 1_000_000; i++) TokenEstimator.estimate(sample);
        long teOps = (long) (1_000_000 / ((System.nanoTime() - t2) / 1_000_000_000.0));
        System.out.printf("TokenEstimator.estimate:  %,d ops/sec%n", teOps);

        // ToolRegistry
        long t3 = System.nanoTime();
        for (int i = 0; i < 1_000_000; i++) toolRegistry.get("web_search");
        long trOps = (long) (1_000_000 / ((System.nanoTime() - t3) / 1_000_000_000.0));
        System.out.printf("ToolRegistry.get:         %,d ops/sec%n", trOps);

        // LoopDetection
        long t4 = System.nanoTime();
        for (int i = 0; i < 100_000; i++) loopDetectionService.detectLoop(1L, "h" + i, List.of("a", "b"));
        long ldOps = (long) (100_000 / ((System.nanoTime() - t4) / 1_000_000_000.0));
        System.out.printf("LoopDetection.detect:     %,d ops/sec%n", ldOps);

        System.out.println("========================================\n");

        assertThat(tbOps).isGreaterThan(500_000);
        assertThat(teOps).isGreaterThan(2_000_000);
        assertThat(trOps).isGreaterThan(1_000_000);
        assertThat(ldOps).isGreaterThan(50_000);
    }
}
