package com.schemaplexai.context.exception;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;

public class VirusDetectedException extends BaseException {

    public VirusDetectedException(String fileName, String virusName) {
        super(ResultCode.FORBIDDEN, "Virus detected in file '" + fileName + "': " + virusName);
    }
}
