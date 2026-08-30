package com.schemaplexai.workflow.service;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;

/**
 * Thrown when a workflow checkpoint's topology hash does not match the current template.
 * This indicates the template was modified after the checkpoint was created,
 * which could lead to silent corruption if the workflow were to resume.
 */
public class TopologyMismatchException extends BaseException {

    public TopologyMismatchException(String message) {
        super(ResultCode.PARAM_ERROR, message);
    }

    public TopologyMismatchException(String message, Throwable cause) {
        super(ResultCode.PARAM_ERROR, message, cause);
    }
}
