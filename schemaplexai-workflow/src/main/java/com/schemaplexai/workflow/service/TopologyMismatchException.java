package com.schemaplexai.workflow.service;

/**
 * Thrown when a workflow checkpoint's topology hash does not match the current template.
 * This indicates the template was modified after the checkpoint was created,
 * which could lead to silent corruption if the workflow were to resume.
 */
public class TopologyMismatchException extends RuntimeException {

    public TopologyMismatchException(String message) {
        super(message);
    }

    public TopologyMismatchException(String message, Throwable cause) {
        super(message, cause);
    }
}
