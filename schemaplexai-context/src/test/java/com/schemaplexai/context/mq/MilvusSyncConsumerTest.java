package com.schemaplexai.context.mq;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.context.service.MilvusSyncService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class MilvusSyncConsumerTest {

    @Mock
    private MilvusSyncService milvusSyncService;

    @InjectMocks
    private MilvusSyncConsumer consumer;

    @Test
    void consume_nullDocId_throwsParamErrorWithoutSync() {
        assertThatThrownBy(() -> consumer.consume(null))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());

        verifyNoInteractions(milvusSyncService);
    }

    @Test
    void consume_delegatesToService() {
        consumer.consume(1L);
        verify(milvusSyncService).syncToMilvus(1L);
    }
}
