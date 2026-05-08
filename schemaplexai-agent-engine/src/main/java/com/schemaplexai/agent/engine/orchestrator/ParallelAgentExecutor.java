package com.schemaplexai.agent.engine.orchestrator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.stream.IntStream;

/**
 * Executes sub-tasks concurrently using {@link CompletableFuture}.
 * Each sub-task is dispatched to the best-matching agent in parallel.
 */
@Slf4j
@Component
public class ParallelAgentExecutor {

    private final AgentRouter agentRouter;
    private final Executor executor;

    public ParallelAgentExecutor(AgentRouter agentRouter, Executor agentExecutionExecutor) {
        this.agentRouter = agentRouter;
        this.executor = agentExecutionExecutor;
    }

    /**
     * Executes all sub-tasks concurrently. Failures in individual sub-tasks do not
     * abort the remaining tasks; they are captured and reported in the results.
     *
     * @param request         the coordination request
     * @param availableAgents available agent capabilities
     * @return list of sub-task results (order matches input order)
     */
    public List<SubTaskResult> execute(CoordinatorAgent.CoordinationRequest request,
                                        List<AgentRouter.AgentCapability> availableAgents) {
        List<CompletableFuture<SubTaskResult>> futures = IntStream.range(0, request.subTasks().size())
                .mapToObj(i -> CompletableFuture.supplyAsync(() -> executeSingleTask(request.subTasks().get(i), availableAgents), executor)
                        .exceptionally(ex -> {
                            log.error("Sub-task '{}' failed with exception: {}", request.subTasks().get(i), ex.getMessage());
                            return new SubTaskResult(request.subTasks().get(i), null, SubTaskStatus.FAILED,
                                    unwrapException(ex));
                        }))
                .toList();

        // Wait for all futures to complete and collect results in order
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        List<SubTaskResult> results = new ArrayList<>(futures.size());
        for (CompletableFuture<SubTaskResult> future : futures) {
            results.add(future.join()); // Already completed after allOf
        }

        return results;
    }

    private SubTaskResult executeSingleTask(String subTask, List<AgentRouter.AgentCapability> availableAgents) {
        var matchedAgent = agentRouter.route(subTask, availableAgents);
        if (matchedAgent.isEmpty()) {
            log.error("No agent available for sub-task: {}", subTask);
            return new SubTaskResult(subTask, null, SubTaskStatus.FAILED, "No matching agent found");
        }

        String agentId = matchedAgent.get().agentId();
        log.info("Dispatching sub-task to agent '{}': {}", agentId, subTask);

        try {
            // In a real implementation, this would dispatch to AgentExecutionEngine
            // and wait for the async result. For now, we record the dispatch.
            return new SubTaskResult(subTask, agentId, SubTaskStatus.COMPLETED, null);
        } catch (Exception e) {
            log.error("Sub-task '{}' failed on agent '{}': {}", subTask, agentId, e.getMessage());
            return new SubTaskResult(subTask, agentId, SubTaskStatus.FAILED, e.getMessage());
        }
    }

    private static String unwrapException(Throwable ex) {
        if (ex instanceof CompletionException ce && ce.getCause() != null) {
            return ce.getCause().getMessage();
        }
        return ex.getMessage();
    }
}
