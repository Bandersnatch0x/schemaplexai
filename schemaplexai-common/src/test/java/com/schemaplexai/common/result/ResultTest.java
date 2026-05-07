package com.schemaplexai.common.result;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResultTest {

    @Test
    void success_withData_returnsCode200() {
        Result<String> result = Result.success("hello");
        assertEquals(200, result.getCode());
        assertEquals("hello", result.getData());
    }

    @Test
    void success_withoutData_returnsCode200() {
        Result<Void> result = Result.success();
        assertEquals(200, result.getCode());
        assertNull(result.getData());
    }

    @Test
    void error_withMessage_returnsCode500() {
        Result<Void> result = Result.error("something went wrong");
        assertEquals(500, result.getCode());
        assertEquals("something went wrong", result.getMessage());
    }

    @Test
    void error_withResultCode_returnsCustomCode() {
        Result<Void> result = Result.error(ResultCode.NOT_FOUND);
        assertEquals(404, result.getCode());
    }

    @Test
    void isSuccess_withCode200_returnsTrue() {
        Result<String> result = Result.success("ok");
        assertTrue(result.isSuccess());
    }

    @Test
    void isSuccess_withCode500_returnsFalse() {
        Result<Void> result = Result.error("fail");
        assertFalse(result.isSuccess());
    }

    @Test
    void error_withCodeAndMessage_returnsCorrectValues() {
        Result<Void> result = Result.error(418, "I'm a teapot");
        assertEquals(418, result.getCode());
        assertEquals("I'm a teapot", result.getMessage());
    }

    @Test
    void timestamp_isSetOnCreation() {
        Result<Void> result = new Result<>();
        assertNotNull(result.getTimestamp());
        assertTrue(result.getTimestamp() > 0);
    }
}
