package com.schemaplexai.agent.engine.chain;

/**
 * Defines a single step in a prompt chain.
 *
 * @param id             unique step identifier
 * @param name           human-readable step name
 * @param promptTemplate prompt template with {@code {variable}} placeholders
 * @param inputMapping   maps previous step outputs to template variables
 *                       (e.g. {@code "step1.output -> {context}"})
 * @param outputKey      names this step's output for downstream steps
 * @param maxRetries     maximum number of retries on failure
 * @param temperature    LLM temperature for this step
 */
public record ChainStep(
        String id,
        String name,
        String promptTemplate,
        String inputMapping,
        String outputKey,
        int maxRetries,
        double temperature
) {
}
