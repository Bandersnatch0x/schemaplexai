package com.schemaplexai.context.controller;

import com.schemaplexai.common.context.TenantContextHolder;
import com.schemaplexai.common.result.Result;
import com.schemaplexai.context.milvus.MilvusVectorCounter;
import com.schemaplexai.context.service.MilvusSyncService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InternalMilvusSyncControllerTest {

    @Mock
    private MilvusSyncService milvusSyncService;

    @Mock
    private ObjectProvider<MilvusVectorCounter> vectorCounterProvider;

    @InjectMocks
    private InternalMilvusSyncController controller;

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    // ------------------------------------------------------------------
    // POST /{docId}
    // ------------------------------------------------------------------

    @Test
    void ensureSynced_missingTenant_returns400WithoutSync() {
        Result<Boolean> result = controller.ensureSynced(1L);

        assertThat(result.getCode()).isEqualTo(400);
        assertThat(result.getMessage()).contains("Tenant ID is required");
        verifyNoInteractions(milvusSyncService);
    }

    @Test
    void ensureSynced_blankTenant_returns400WithoutSync() {
        TenantContextHolder.setTenantId("   ");

        Result<Boolean> result = controller.ensureSynced(1L);

        assertThat(result.getCode()).isEqualTo(400);
        verifyNoInteractions(milvusSyncService);
    }

    @Test
    void ensureSynced_withTenant_delegatesIdempotentReSync() {
        TenantContextHolder.setTenantId("tenant-1");

        Result<Boolean> result = controller.ensureSynced(42L);

        assertThat(result.isSuccess()).isTrue();
        verify(milvusSyncService).reSyncDoc(42L);
    }

    @Test
    void ensureSynced_serviceFailure_propagates() {
        TenantContextHolder.setTenantId("tenant-1");
        doThrow(new com.schemaplexai.common.exception.BaseException(
                com.schemaplexai.common.result.ResultCode.NOT_FOUND, "Knowledge document not found: 7"))
                .when(milvusSyncService).reSyncDoc(7L);

        assertThatThrownBy(() -> controller.ensureSynced(7L))
                .isInstanceOf(com.schemaplexai.common.exception.BaseException.class);
    }

    // ------------------------------------------------------------------
    // GET /docs/{docId}/vector-count
    // ------------------------------------------------------------------

    @Test
    void vectorCount_missingTenant_returns400() {
        Result<Long> result = controller.vectorCount(1L);

        assertThat(result.getCode()).isEqualTo(400);
        verifyNoInteractions(vectorCounterProvider);
    }

    @Test
    void vectorCount_milvusDisabled_returns503() {
        TenantContextHolder.setTenantId("tenant-1");
        when(vectorCounterProvider.getIfAvailable()).thenReturn(null);

        Result<Long> result = controller.vectorCount(1L);

        assertThat(result.getCode()).isEqualTo(503);
        assertThat(result.getMessage()).contains("Milvus is disabled");
    }

    @Test
    void vectorCount_milvusEnabled_returnsCount() {
        TenantContextHolder.setTenantId("tenant-1");
        MilvusVectorCounter counter = mock(MilvusVectorCounter.class);
        when(vectorCounterProvider.getIfAvailable()).thenReturn(counter);
        when(counter.countByDocId(5L, "tenant-1")).thenReturn(3L);

        Result<Long> result = controller.vectorCount(5L);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEqualTo(3L);
    }
}
