package com.schemaplexai.agent.engine.dto;

import java.util.List;

/**
 * Result of schema validation with per-table/per-column error reporting.
 */
public record SchemaValidationResult(
        boolean valid,
        List<SchemaValidationError> errors
) {
    public boolean isValid() {
        return valid;
    }
}