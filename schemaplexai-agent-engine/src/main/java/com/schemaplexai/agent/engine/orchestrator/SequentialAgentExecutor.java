package com.schemaplexai.agent.engine.orchestrator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Executes sub-tasks sequentially, passing each output as context to the next task.
 */
@Slf4j
@Component
public class SequentialAgentExecutor {

    private final AgentRouter agentRouter;

    public SequentialAgentExecutor(AgentRouter agentRouter) {
        this.agentRouter = agentRouter;
    }

    /**
     * Executes sub-tasks one by one in order. Each sub-task receives the accumulated
     * results from prior tasks as additional context.
     *
     * @param request         the coordination request
     * @param availableAgents available agent capabilities
     * @return list of sub-task results in execution order
     */
    public List<SubTaskResult> execute(CoordinatorAgent.CoordinationRequest request,
                                        List<AgentRouter.AgentCapability> availableAgents) {
        List<SubTaskResult> results = new ArrayList<>();
        StringBuilder accumulatedContext = new StringBuilder();

        for (int i = 0; i < request.subTasks().size(); i++) {
            String subTask = request.subTasks().get(i);
            String taskWithContext = subTask;
            if (!accumulatedContext.isEmpty()) {
                taskWithContext = subTask + "\n\nPrevious results:\n" + accumulatedContext;
            }

            log.info("Executing sub-task {}/{}: {}", i + 1, request.subTasks().size(), subTask);

            var matchedAgent = agentRouter.route(taskWithContext, availableAgents);
            if (matchedAgent.isEmpty()) {
                log.error("No agent available for sub-task: {}", subTask);
                results.add(new SubTaskResult(subTask, null, SubTaskStatus.FAILED, "No matching agent found"));
                break;
            }

            String agentId = matchedAgent.get().agentId();
            try {
                // In a real implementation, this would dispatch to AgentExecutionEngine
                // and wait for the result. For now, we record the dispatch.
                log.info("Dispatching sub-task to agent '{}': {}", agentId, subTask);
                results.add(new SubTaskResult(subTask, agentId, SubTaskStatus.COMPLETED, null));
                accumulatedContext.append("[").append(agentId).append("] ").append(subTask).append("\n");
            } catch (Exception e) {
                log.error("Sub-task '{}' failed on agent '{}': {}", subTask, agentId, e.getMessage());
                results.add(new SubTaskResult(subTask, agentId, SubTaskStatus.FAILED, e.getMessage()));
                break; // Sequential execution stops on failure
            }
        }

        return results;
    }
}
