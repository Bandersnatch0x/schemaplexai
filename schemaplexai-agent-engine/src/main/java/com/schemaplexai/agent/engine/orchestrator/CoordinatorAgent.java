package com.schemaplexai.agent.engine.orchestrator;

import com.schemaplexai.agent.engine.AgentExecutionEngine;
import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates multi-agent workflows by decomposing a parent task into sub-tasks
 * and dispatching them to specialized agents via the {@link AgentRouter}.
 */
@Slf4j
@Component
public class CoordinatorAgent {

    private final AgentExecutionEngine executionEngine;
    private final AgentRouter agentRouter;
    private final SequentialAgentExecutor sequentialExecutor;
    private final ParallelAgentExecutor parallelExecutor;

    public CoordinatorAgent(AgentExecutionEngine executionEngine,
                            AgentRouter agentRouter,
                            SequentialAgentExecutor sequentialExecutor,
                            ParallelAgentExecutor parallelAgentExecutor) {
        this.executionEngine = executionEngine;
        this.agentRouter = agentRouter;
        this.sequentialExecutor = sequentialExecutor;
        this.parallelExecutor = parallelAgentExecutor;
    }

    /**
     * Coordination strategy for sub-task execution.
     */
    public enum CoordinationStrategy {
        SEQUENTIAL,
        PARALLEL
    }

    /**
     * Request to coordinate multiple sub-tasks across agents.
     *
     * @param parentExecutionId the parent execution that triggered coordination
     * @param tenantId          tenant context
     * @param subTasks          list of sub-task descriptions
     * @param strategy          execution strategy (SEQUENTIAL or PARALLEL)
     */
    public record CoordinationRequest(
            Long parentExecutionId,
            String tenantId,
            List<String> subTasks,
            CoordinationStrategy strategy
    ) {
        public CoordinationRequest {
            if (parentExecutionId == null) {
                throw new IllegalArgumentException("parentExecutionId must not be null");
            }
            if (tenantId == null || tenantId.isBlank()) {
                throw new IllegalArgumentException("tenantId must not be blank");
            }
            if (subTasks == null || subTasks.isEmpty()) {
                throw new IllegalArgumentException("subTasks must not be empty");
            }
            if (strategy == null) {
                strategy = CoordinationStrategy.SEQUENTIAL;
            }
        }
    }

    /**
     * Result of a coordination effort.
     *
     * @param subTaskResults list of individual sub-task results
     * @param allSucceeded   true if every sub-task completed successfully
     */
    public record CoordinationResult(
            List<SubTaskResult> subTaskResults,
            boolean allSucceeded
    ) {}

    /**
     * Coordinates the execution of sub-tasks across specialized agents.
     *
     * @param request          coordination request
     * @param availableAgents  agent capabilities available for routing
     * @return coordination result with per-sub-task outcomes
     */
    public CoordinationResult coordinate(CoordinationRequest request,
                                          List<AgentRouter.AgentCapability> availableAgents) {
        log.info("Starting coordination for parent execution {} with {} sub-tasks, strategy={}",
                request.parentExecutionId(), request.subTasks().size(), request.strategy());

        List<SubTaskResult> results;

        if (request.strategy() == CoordinationStrategy.PARALLEL) {
            results = parallelExecutor.execute(request, availableAgents);
        } else {
            results = sequentialExecutor.execute(request, availableAgents);
        }

        boolean allSucceeded = results.stream().allMatch(SubTaskResult::isSuccess);

        log.info("Coordination complete for parent execution {}: {}/{} succeeded",
                request.parentExecutionId(),
                results.stream().filter(SubTaskResult::isSuccess).count(),
                results.size());

        return new CoordinationResult(results, allSucceeded);
    }

    /**
     * Dispatches a sub-agent execution via the execution engine.
     *
     * @param agentId  target agent ID
     * @param tenantId tenant context
     * @param prompt   the sub-task prompt
     * @return the created execution entity
     */
    public SfAgentExecution dispatchSubAgent(Long agentId, String tenantId, String prompt) {
        log.info("Dispatching sub-agent execution: agentId={}, tenantId={}", agentId, tenantId);
        return executionEngine.startExecution(agentId, tenantId, prompt);
    }
}
