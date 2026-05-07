package com.schemaplexai.agent.engine.skill;

import com.schemaplexai.common.exception.BaseException;

/**
 * Exception thrown when skill markdown validation fails.
 * Covers: missing fields, length violations, HTML injection attempts.
 */
public class ValidationException extends BaseException {

    /** Agent module validation error code (2000-2999 range). */
    private static final int VALIDATION_ERROR_CODE = 2001;

    public ValidationException(String message) {
        super(VALIDATION_ERROR_CODE, message);
    }
}
