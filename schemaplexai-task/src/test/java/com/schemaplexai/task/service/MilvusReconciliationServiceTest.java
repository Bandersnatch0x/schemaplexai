package com.schemaplexai.task.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.common.constants.CommonConstants;
import com.schemaplexai.task.mapper.MilvusReconciliationMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MilvusReconciliationServiceTest {

    @Mock
    private MilvusReconciliationMapper reconciliationMapper;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private MilvusReconciliationService reconciliationService;

    @Test
    void reconcilePendingDocuments_dispatchesSyncMessageForEachPendingDocument() throws Exception {
        PendingMilvusDocument first = new PendingMilvusDocument(11L, "tenant-a");
        PendingMilvusDocument second = new PendingMilvusDocument(12L, "tenant-b");
        when(reconciliationMapper.findPendingDocuments(50)).thenReturn(List.of(first, second));
        when(objectMapper.writeValueAsString(any())).thenReturn("payload");

        int dispatched = reconciliationService.reconcilePendingDocuments(50);

        assertThat(dispatched).isEqualTo(2);
        verify(rabbitTemplate, org.mockito.Mockito.times(2)).convertAndSend(
                eq(CommonConstants.EXCHANGE_SCHEMAPLEXAI),
                eq(CommonConstants.RK_MILVUS_SYNC),
                eq("payload")
        );

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(objectMapper, org.mockito.Mockito.times(2)).writeValueAsString(payloadCaptor.capture());
        assertThat(payloadCaptor.getAllValues())
                .allSatisfy(payload -> assertThat(payload).isInstanceOf(Map.class));

        @SuppressWarnings("unchecked")
        Map<String, Object> firstPayload = (Map<String, Object>) payloadCaptor.getAllValues().get(0);
        assertThat(firstPayload)
                .containsEntry("operation", "SYNC_DOC")
                .containsEntry("docId", 11L)
                .containsEntry("tenantId", "tenant-a");
    }

    @Test
    void reconcilePendingDocuments_returnsZeroWhenNoPendingDocuments() {
        when(reconciliationMapper.findPendingDocuments(100)).thenReturn(List.of());

        int dispatched = reconciliationService.reconcilePendingDocuments(100);

        assertThat(dispatched).isZero();
        verify(rabbitTemplate, never()).convertAndSend(any(String.class), any(String.class), any(Object.class));
    }

    @Test
    void reconcilePendingDocuments_rejectsNonPositiveBatchSize() {
        assertThatThrownBy(() -> reconciliationService.reconcilePendingDocuments(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("batchSize");

        verifyNoInteractions(reconciliationMapper, rabbitTemplate, objectMapper);
    }
}
