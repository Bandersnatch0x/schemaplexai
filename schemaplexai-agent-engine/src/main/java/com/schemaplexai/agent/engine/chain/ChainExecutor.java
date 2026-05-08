package com.schemaplexai.agent.engine.chain;

import com.schemaplexai.agent.engine.model.AiModelRouter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Executes a {@link ChainDefinition} by iterating through its steps sequentially,
 * resolving templates, calling the LLM via {@link AiModelRouter}, and accumulating outputs.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChainExecutor {

    private final AiModelRouter aiModelRouter;

    /**
     * Executes all steps in the given chain definition sequentially.
     *
     * @param chain   the chain definition to execute
     * @param modelId the LLM model identifier to use for all steps
     * @return a {@link ChainExecutionResult} containing outputs and status
     */
    public ChainExecutionResult execute(ChainDefinition chain, String modelId) {
        Instant start = Instant.now();
        ChainExecutionContext context = new ChainExecutionContext(chain.initialInputs());

        log.info("Starting chain execution: id={}, name={}, steps={}", chain.id(), chain.name(), chain.steps().size());

        for (ChainStep step : chain.steps()) {
            String resolvedPrompt = resolvePrompt(step, context);
            log.debug("Executing step: id={}, name={}, template resolved length={}", step.id(), step.name(), resolvedPrompt.length());

            String output = executeStepWithRetries(step, resolvedPrompt, modelId);

            if (output == null) {
                Duration elapsed = Duration.between(start, Instant.now());
                log.warn("Chain failed at step: id={}, name={}", step.id(), step.name());
                return ChainExecutionResult.failure(
                        chain.id(),
                        context.getAllOutputs(),
                        step.id(),
                        "Step '" + step.name() + "' (id=" + step.id() + ") failed after " + step.maxRetries() + " retries",
                        elapsed
                );
            }

            context.setOutput(step.outputKey(), output);
            log.debug("Step completed: id={}, outputKey={}, outputLength={}", step.id(), step.outputKey(), output.length());
        }

        Duration elapsed = Duration.between(start, Instant.now());
        log.info("Chain completed successfully: id={}, duration={}ms", chain.id(), elapsed.toMillis());
        return ChainExecutionResult.success(chain.id(), context.getAllOutputs(), elapsed);
    }

    /**
     * Resolves a step's prompt template using the current execution context.
     * First applies input mappings, then resolves remaining placeholders.
     */
    private String resolvePrompt(ChainStep step, ChainExecutionContext context) {
        applyInputMapping(step.inputMapping(), context);
        return context.resolveTemplate(step.promptTemplate());
    }

    /**
     * Applies input mapping to the context, making previous step outputs available
     * under the mapped variable names.
     * <p>
     * Format: {@code "stepOutputKey -> {variableName}"} — supports multiple mappings
     * separated by semicolons.
     */
    void applyInputMapping(String inputMapping, ChainExecutionContext context) {
        if (inputMapping == null || inputMapping.isBlank()) {
            return;
        }

        String[] mappings = inputMapping.split(";");
        for (String mapping : mappings) {
            String trimmed = mapping.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            String[] parts = trimmed.split("->", 2);
            if (parts.length != 2) {
                log.warn("Invalid input mapping format: '{}', expected 'sourceKey -> {{targetVar}}'", trimmed);
                continue;
            }

            String sourceKey = parts[0].trim();
            String targetPlaceholder = parts[1].trim();

            // Extract variable name from {varName}
            if (targetPlaceholder.startsWith("{") && targetPlaceholder.endsWith("}")) {
                String targetVar = targetPlaceholder.substring(1, targetPlaceholder.length() - 1);
                String sourceValue = context.getOutput(sourceKey);
                if (sourceValue != null) {
                    context.setOutput(targetVar, sourceValue);
                } else {
                    log.debug("Source key '{}' not found in context for input mapping", sourceKey);
                }
            } else {
                log.warn("Invalid target placeholder format: '{}', expected '{{variableName}}'", targetPlaceholder);
            }
        }
    }

    /**
     * Executes a step with retry logic.
     *
     * @return the LLM output, or {@code null} if all retries are exhausted
     */
    private String executeStepWithRetries(ChainStep step, String resolvedPrompt, String modelId) {
        int attempts = Math.max(1, step.maxRetries() + 1);

        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                log.debug("Step '{}' attempt {}/{}", step.id(), attempt, attempts);
                return aiModelRouter.generateWithFallback(resolvedPrompt, modelId, step.temperature());
            } catch (Exception e) {
                log.warn("Step '{}' failed on attempt {}/{}: {}", step.id(), attempt, attempts, e.getMessage());
                if (attempt == attempts) {
                    log.error("Step '{}' exhausted all {} attempts", step.id(), attempts, e);
                    return null;
                }
            }
        }
        return null;
    }
}
