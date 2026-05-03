package com.schemaplexai.agent.engine.evaluation;

/**
 * Evaluator interface - defines the contract for evaluating Agent execution traces.
 */
public interface Evaluator {

    /**
     * Evaluate an execution trace.
     *
     * @param trace the execution trace to evaluate
     * @return the evaluation result
     */
    EvaluationResult evaluate(AgentExecutionTrace trace);

    /**
     * Compare two evaluation results to determine trend.
     *
     * @param baseline the baseline evaluation result
     * @param current  the current evaluation result
     * @return the delta analysis
     */
    EvaluationDelta compare(EvaluationResult baseline, EvaluationResult current);

    /**
     * Get the evaluator name.
     *
     * @return evaluator name
     */
    String getName();
}
