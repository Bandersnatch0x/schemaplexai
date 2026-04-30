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
}
