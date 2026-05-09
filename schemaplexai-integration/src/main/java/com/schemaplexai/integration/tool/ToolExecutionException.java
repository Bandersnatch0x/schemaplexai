package com.schemaplexai.integration.tool;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;

public class ToolExecutionException extends BaseException {

    public ToolExecutionException(String message) {
        super(ResultCode.TOOL_EXECUTION_FAILED, message);
    }

    public ToolExecutionException(String message, Throwable cause) {
        super(ResultCode.TOOL_EXECUTION_FAILED, message, cause);
    }
}
