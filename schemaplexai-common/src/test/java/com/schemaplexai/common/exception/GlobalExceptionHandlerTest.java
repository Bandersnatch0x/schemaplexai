package com.schemaplexai.common.exception;

import com.schemaplexai.common.result.Result;
import com.schemaplexai.common.result.ResultCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link GlobalExceptionHandler}. Verifies that every branch maps
 * exceptions onto the {@link Result} envelope without leaking internals.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleBase_returnsCodeAndMessage() {
        BaseException ex = new BaseException(ResultCode.NOT_FOUND, "agent missing");

        Result<Void> result = handler.handleBase(ex);

        assertThat(result.getCode()).isEqualTo(ResultCode.NOT_FOUND.getCode());
        assertThat(result.getMessage()).isEqualTo("agent missing");
    }

    @Test
    void handleBase_withCustomCode() {
        BaseException ex = new BaseException(3001, "agent not found");

        Result<Void> result = handler.handleBase(ex);

        assertThat(result.getCode()).isEqualTo(3001);
        assertThat(result.getMessage()).isEqualTo("agent not found");
    }

    @Test
    void handleValidation_concatenatesFieldErrors() throws Exception {
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(new Object(), "target");
        bindingResult.addError(new FieldError("target", "name", "must not be blank"));
        bindingResult.addError(new FieldError("target", "age", "must be >= 0"));
        Method method = Sample.class.getDeclaredMethod("noop", String.class);
        MethodParameter parameter = new MethodParameter(method, 0);
        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(parameter, bindingResult);

        Result<Void> result = handler.handleValidation(ex);

        assertThat(result.getCode()).isEqualTo(ResultCode.PARAM_ERROR.getCode());
        assertThat(result.getMessage()).contains("name:must not be blank");
        assertThat(result.getMessage()).contains("age:must be >= 0");
    }

    @Test
    void handleBind_concatenatesFieldErrors() {
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(new Object(), "target");
        bindingResult.addError(new FieldError("target", "email", "must be a valid email"));
        BindException ex = new BindException(bindingResult);

        Result<Void> result = handler.handleBind(ex);

        assertThat(result.getCode()).isEqualTo(ResultCode.PARAM_ERROR.getCode());
        assertThat(result.getMessage()).isEqualTo("email:must be a valid email");
    }

    @Test
    void handleConstraint_returnsParamError() {
        Set<ConstraintViolation<?>> violations = new HashSet<>();
        ConstraintViolationException ex =
                new ConstraintViolationException("size must be between 1 and 100", violations);

        Result<Void> result = handler.handleConstraint(ex);

        assertThat(result.getCode()).isEqualTo(ResultCode.PARAM_ERROR.getCode());
        assertThat(result.getMessage()).contains("size must be between 1 and 100");
    }

    @Test
    void handleAll_doesNotLeakOriginalMessage() {
        Exception ex = new RuntimeException("connection string=jdbc:postgresql://secret-host/db");

        Result<Void> result = handler.handleAll(ex);

        assertThat(result.getCode()).isEqualTo(ResultCode.ERROR.getCode());
        assertThat(result.getMessage()).isEqualTo("Internal error");
        assertThat(result.getMessage()).doesNotContain("secret-host");
    }

    @SuppressWarnings("unused")
    private static class Sample {
        void noop(String input) {}
    }
}
