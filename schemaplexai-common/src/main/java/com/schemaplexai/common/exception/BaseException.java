package com.schemaplexai.common.exception;

import com.schemaplexai.common.result.ResultCode;
import lombok.Getter;

@Getter
public class BaseException extends RuntimeException {

    private final Integer code;

    public BaseException(String message) {
        super(message);
        this.code = ResultCode.ERROR.getCode();
    }

    public BaseException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BaseException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }

    public BaseException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public BaseException(ResultCode resultCode, String message, Throwable cause) {
        super(message, cause);
        this.code = resultCode.getCode();
    }
}
