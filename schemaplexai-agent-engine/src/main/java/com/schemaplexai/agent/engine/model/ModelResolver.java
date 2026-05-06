package com.schemaplexai.agent.engine.model;

import com.schemaplexai.agent.engine.entity.SfAgentExecution;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Resolves the LLM model ID for an agent execution.
 *
 * Resolution order (highest to lowest priority):
 * 1. Execution metadata key "modelId" (set at execution creation time, e.g. from SfAgentConfig)
 * 2. Application configuration {@code agent.model.default-model}
 * 3. Hardcoded fallback "gpt-4" (documented, for backward compatibility)
 *
 * <p>The resolved model ID is validated against a known set of supported models.
 * If validation fails, the fallback model is used and a warning is logged.
 */
@Slf4j
@Component
public class ModelResolver {

    /**
     * Documented fallback model for backward compatibility.
     * Used only when no execution metadata or application config is available.
     */
    public static final String FALLBACK_MODEL = "gpt-4";

    private static final String METADATA_KEY_MODEL_ID = "modelId";

    private static final Set<String> SUPPORTED_MODELS = Set.of(
            "gpt-4",
            "gpt-4o",
            "gpt-4o-mini",
            "gpt-4-turbo",
            "gpt-3.5-turbo",
            "claude-3-opus",
            "claude-3-sonnet",
            "claude-3-haiku",
            "claude-3-5-sonnet"
    );

    private final String defaultModel;

    public ModelResolver(
            @Value("${agent.model.default-model:}") String defaultModel) {
        this.defaultModel = defaultModel != null && !defaultModel.isBlank()
                ? defaultModel.trim()
                : null;
    }

    /**
     * Resolves the model ID for the given execution.
     *
     * @param execution the current agent execution
     * @return a non-null, non-blank model identifier
     */
    public String resolve(SfAgentExecution execution) {
        if (execution == null) {
            log.warn("Execution is null, using fallback model '{}'", FALLBACK_MODEL);
            return FALLBACK_MODEL;
        }

        // Priority 1: execution metadata (e.g. from SfAgentConfig.modelId at creation time)
        Object metadataModel = execution.getMetadata(METADATA_KEY_MODEL_ID);
        if (metadataModel instanceof String modelId && !modelId.isBlank()) {
            String resolved = validateAndNormalize(modelId);
            log.debug("Resolved model '{}' from execution metadata for execution {}",
                    resolved, execution.getId());
            return resolved;
        }

        // Priority 2: application configuration
        if (defaultModel != null) {
            String resolved = validateAndNormalize(defaultModel);
            log.debug("Resolved model '{}' from application config for execution {}",
                    resolved, execution.getId());
            return resolved;
        }

        // Priority 3: documented fallback (backward-compatible)
        log.debug("Using fallback model '{}' for execution {} (no config available)",
                FALLBACK_MODEL, execution.getId());
        return FALLBACK_MODEL;
    }

    /**
     * Validates that the given model ID is supported.
     * If not supported, logs a warning and returns the fallback model.
     *
     * @param modelId the raw model identifier
     * @return the validated model identifier
     */
    private String validateAndNormalize(String modelId) {
        String normalized = modelId.trim().toLowerCase();
        if (SUPPORTED_MODELS.contains(normalized)) {
            return normalized;
        }
        log.warn("Unsupported model ID '{}', falling back to '{}'", modelId, FALLBACK_MODEL);
        return FALLBACK_MODEL;
    }

    /**
     * Returns whether the given model ID is in the supported set.
     *
     * @param modelId the model identifier to check
     * @return true if supported
     */
    public boolean isSupported(String modelId) {
        if (modelId == null || modelId.isBlank()) {
            return false;
        }
        return SUPPORTED_MODELS.contains(modelId.trim().toLowerCase());
    }
}
