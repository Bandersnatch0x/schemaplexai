package com.schemaplexai.context.service.impl;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DisabledFileStorageServiceTest {

    private final DisabledFileStorageService service = new DisabledFileStorageService();

    @Test
    void upload_throwsInternalErrorWhenStorageIsDisabled() {
        assertThatThrownBy(() -> service.upload(
                "tenant-1",
                "file.txt",
                "text/plain",
                new ByteArrayInputStream("hello".getBytes()),
                5
        ))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.INTERNAL_ERROR.getCode());
    }
}
