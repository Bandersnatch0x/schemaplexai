package com.schemaplexai.context.service.impl;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NoOpMilvusSyncServiceImplTest {

    private final NoOpMilvusSyncServiceImpl service = new NoOpMilvusSyncServiceImpl();

    @Test
    void syncToMilvus_nullDocId_throwsParamError() {
        assertThatThrownBy(() -> service.syncToMilvus(null))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void deleteByDocId_nullDocId_throwsParamError() {
        assertThatThrownBy(() -> service.deleteByDocId(null))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void reSyncDoc_nullDocId_throwsParamError() {
        assertThatThrownBy(() -> service.reSyncDoc(null))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void syncToMilvus_doesNotThrow() {
        service.syncToMilvus(1L);
    }
}
