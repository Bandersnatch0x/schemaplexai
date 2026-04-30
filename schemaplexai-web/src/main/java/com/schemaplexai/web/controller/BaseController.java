package com.schemaplexai.web.controller;

import com.schemaplexai.common.result.Result;

public abstract class BaseController {

    protected <T> Result<T> success(T data) {
        return Result.success(data);
    }

    protected Result<Void> success() {
        return Result.success();
    }

    protected <T> Result<T> error(String message) {
        return Result.error(message);
    }

    protected <T> Result<T> error(Integer code, String message) {
        return Result.error(code, message);
    }
}
