package com.schemaplexai.common.exception;

import com.schemaplexai.common.result.ResultCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BaseExceptionTest {

    @Test
    void constructor_setsCodeAndMessage() {
        BaseException ex = new BaseException(1001, "agent not found");
        assertEquals(1001, ex.getCode());
        assertEquals("agent not found", ex.getMessage());
    }

    @Test
    void constructor_withResultCode() {
        BaseException ex = new BaseException(ResultCode.NOT_FOUND);
        assertEquals(404, ex.getCode());
    }
}
