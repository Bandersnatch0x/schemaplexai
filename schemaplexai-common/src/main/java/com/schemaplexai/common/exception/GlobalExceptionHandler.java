package com.schemaplexai.common.exception;

import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Global exception handler converting all exceptions thrown from MVC controllers
 * into a uniform {@link Result} envelope.
 *
 * <p>Registered automatically for every Spring Boot service that depends on
 * {@code schemaplexai-common} via
 * {@link CommonExceptionAutoConfiguration}, so no manual {@code @ComponentScan}
 * change is required in downstream service main classes.</p>
 *
 * <p><b>Note:</b> This handler targets Spring MVC ({@code @RestControllerAdvice}).
 * The WebFlux-based gateway is not affected and should handle errors via its
 * own {@code ErrorWebExceptionHandler}.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Domain-level exceptions: pass through code + message verbatim.
     */
    @ExceptionHandler(BaseException.class)
    public Result<Void> handleBase(BaseException e) {
        log.warn("Business exception: code={}, msg={}", e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * @Valid on @RequestBody DTOs.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ":" + fe.getDefaultMessage())
                .collect(Collectors.joining(","));
        log.warn("Validation failed: {}", msg);
        return Result.error(ResultCode.PARAM_ERROR.getCode(), msg);
    }

    /**
     * @Valid on form / query-string binding.
     */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBind(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ":" + fe.getDefaultMessage())
                .collect(Collectors.joining(","));
        log.warn("Bind failed: {}", msg);
        return Result.error(ResultCode.PARAM_ERROR.getCode(), msg);
    }

    /**
     * @Validated on method-level (path/query params, service methods).
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraint(ConstraintViolationException e) {
        log.warn("Constraint violation: {}", e.getMessage());
        return Result.error(ResultCode.PARAM_ERROR.getCode(), e.getMessage());
    }

    /**
     * Catch-all. Never leak the underlying message — log full stack server-side,
     * return a generic message to the client.
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleAll(Exception e) {
        log.error("Unhandled exception", e);
        return Result.error(ResultCode.ERROR.getCode(), "Internal error");
    }
}
