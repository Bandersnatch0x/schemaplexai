package com.schemaplexai.common.controller;

import com.schemaplexai.common.result.Result;

/**
 * Base controller providing convenience methods for wrapping responses in Result.
 * All REST controllers should extend this class for consistent response formatting.
 */
public abstract class BaseController {

    public <T> Result<T> success(T data) {
        return Result.success(data);
    }

    public Result<Void> success() {
        return Result.success();
    }

    public <T> Result<T> error(String message) {
        return Result.error(message);
    }

    public <T> Result<T> error(Integer code, String message) {
        return Result.error(code, message);
    }
}
