package com.schemaplexai.task.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.common.constants.CommonConstants;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

    @Mock
    private ContextServiceClient contextServiceClient;

    @InjectMocks
    private MilvusReconciliationService reconciliationService;

    @Test
    void reconcilePendingDocuments_dispatchesSyncMessageForEachPendingDocument() throws Exception {
        PendingMilvusDocument first = new PendingMilvusDocument(11L, "tenant-a");
        PendingMilvusDocument second = new PendingMilvusDocument(12L, "tenant-b");
        when(reconciliationMapper.findPendingDocuments(50)).thenReturn(List.of(first, second));
        when(reconciliationMapper.findSyncedDocuments(50)).thenReturn(List.of());
        when(objectMapper.writeValueAsString(any())).thenReturn("payload");

        int dispatched = reconciliationService.reconcilePendingDocuments(50);

        assertThat(dispatched).isEqualTo(2);
        verify(rabbitTemplate, times(2)).convertAndSend(
                eq(CommonConstants.EXCHANGE_SCHEMAPLEXAI),
                eq(CommonConstants.RK_MILVUS_SYNC),
                eq("payload")
        );

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(objectMapper, times(2)).writeValueAsString(payloadCaptor.capture());
        assertThat(payloadCaptor.getAllValues())
                .allSatisfy(payload -> assertThat(payload).isInstanceOf(Map.class));

        @SuppressWarnings("unchecked")
        Map<String, Object> firstPayload = (Map<String, Object>) payloadCaptor.getAllValues().get(0);
        assertThat(firstPayload)
                .containsEntry("operation", "SYNC_DOC")
                .containsEntry("docId", 11L)
                .containsEntry("tenantId", "tenant-a");
        // Unique per dispatch so the MQ idempotency layer never swallows a later retry.
        assertThat((String) firstPayload.get("idempotencyKey")).matches("milvus-sync-doc-11-\\d+");
    }

    @Test
    void reconcilePendingDocuments_returnsZeroWhenNoPendingDocuments() {
        when(reconciliationMapper.findPendingDocuments(100)).thenReturn(List.of());
        when(reconciliationMapper.findSyncedDocuments(100)).thenReturn(List.of());

        int dispatched = reconciliationService.reconcilePendingDocuments(100);

        assertThat(dispatched).isZero();
        verify(rabbitTemplate, never()).convertAndSend(any(String.class), any(String.class), any(Object.class));
    }

    @Test
    void reconcilePendingDocuments_rejectsNonPositiveBatchSize() {
        assertThatThrownBy(() -> reconciliationService.reconcilePendingDocuments(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("batchSize");

        verifyNoInteractions(reconciliationMapper, rabbitTemplate, objectMapper, contextServiceClient);
    }

    // ------------------------------------------------------------------
    // PG <-> Milvus count comparison (real reconciliation)
    // ------------------------------------------------------------------

    @Test
    void comparison_syncedDocMissingVectors_dispatchesRepair() throws Exception {
        when(reconciliationMapper.findPendingDocuments(10)).thenReturn(List.of());
        when(reconciliationMapper.findSyncedDocuments(10))
                .thenReturn(List.of(new PendingMilvusDocument(21L, "tenant-a")));
        when(contextServiceClient.countVectors(21L, "tenant-a")).thenReturn(0L);
        when(objectMapper.writeValueAsString(any())).thenReturn("payload");

        int dispatched = reconciliationService.reconcilePendingDocuments(10);

        assertThat(dispatched).isEqualTo(1);
        verify(rabbitTemplate).convertAndSend(
                eq(CommonConstants.EXCHANGE_SCHEMAPLEXAI),
                eq(CommonConstants.RK_MILVUS_SYNC),
                eq("payload"));
    }

    @Test
    void comparison_syncedDocWithVectors_noRepair() {
        when(reconciliationMapper.findPendingDocuments(10)).thenReturn(List.of());
        when(reconciliationMapper.findSyncedDocuments(10))
                .thenReturn(List.of(new PendingMilvusDocument(21L, "tenant-a")));
        when(contextServiceClient.countVectors(21L, "tenant-a")).thenReturn(5L);

        int dispatched = reconciliationService.reconcilePendingDocuments(10);

        assertThat(dispatched).isZero();
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void comparison_countUnavailable_skipsGracefullyWithoutFailingJob() {
        when(reconciliationMapper.findPendingDocuments(10)).thenReturn(List.of());
        when(reconciliationMapper.findSyncedDocuments(10))
                .thenReturn(List.of(new PendingMilvusDocument(21L, "tenant-a")));
        when(contextServiceClient.countVectors(anyLong(), anyString()))
                .thenThrow(new BaseException(ResultCode.INTERNAL_ERROR, "Milvus is disabled"));

        int dispatched = reconciliationService.reconcilePendingDocuments(10);

        assertThat(dispatched).isZero();
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void comparison_dispatchedRepairPayloadCarriesTenantAndDoc() throws Exception {
        when(reconciliationMapper.findPendingDocuments(10)).thenReturn(List.of());
        when(reconciliationMapper.findSyncedDocuments(10))
                .thenReturn(List.of(new PendingMilvusDocument(31L, "tenant-b")));
        when(contextServiceClient.countVectors(31L, "tenant-b")).thenReturn(0L);
        when(objectMapper.writeValueAsString(any())).thenReturn("payload");

        reconciliationService.reconcilePendingDocuments(10);

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(objectMapper).writeValueAsString(payloadCaptor.capture());
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) payloadCaptor.getValue();
        assertThat(payload)
                .containsEntry("operation", "SYNC_DOC")
                .containsEntry("docId", 31L)
                .containsEntry("tenantId", "tenant-b");
    }
}
