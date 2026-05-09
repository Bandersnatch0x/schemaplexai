package com.schemaplexai.agent.engine.dto;

/**
 * Detailed schema validation error record.
 */
public record SchemaValidationError(
        String errorCode,
        String message
) {
}