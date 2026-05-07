package com.schemaplexai.context.mq;

import com.schemaplexai.context.service.MilvusSyncService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MilvusSyncConsumerTest {

    @Mock
    private MilvusSyncService milvusSyncService;

    @InjectMocks
    private MilvusSyncConsumer consumer;

    @Test
    void consume_delegatesToService() {
        consumer.consume(1L);
        verify(milvusSyncService).syncToMilvus(1L);
    }
}
