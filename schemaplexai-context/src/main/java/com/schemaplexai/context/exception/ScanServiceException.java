package com.schemaplexai.context.exception;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;

public class ScanServiceException extends BaseException {

    public ScanServiceException(String message) {
        super(ResultCode.INTERNAL_ERROR, message);
    }

    public ScanServiceException(String message, Throwable cause) {
        super(ResultCode.INTERNAL_ERROR, message, cause);
    }
}
