package com.schemaplexai.context.milvus;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.context.config.MilvusProperties;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.ConsistencyLevel;
import io.milvus.v2.service.vector.request.QueryReq;
import io.milvus.v2.service.vector.response.QueryResp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests with the Milvus client mocked — no real Milvus required.
 */
@ExtendWith(MockitoExtension.class)
class MilvusVectorCounterTest {

    @Mock
    private MilvusClientV2 milvusClient;

    @Mock
    private MilvusProperties milvusProperties;

    @InjectMocks
    private MilvusVectorCounter counter;

    @BeforeEach
    void setUp() {
        lenient().when(milvusProperties.getCollectionName()).thenReturn("test_collection");
        lenient().when(milvusProperties.getConsistencyLevel()).thenReturn(ConsistencyLevel.STRONG);
    }

    @Test
    void countByDocId_nullDocId_throwsParamError() {
        assertThatThrownBy(() -> counter.countByDocId(null, "tenant-1"))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> assertThat(((BaseException) ex).getCode())
                        .isEqualTo(ResultCode.PARAM_ERROR.getCode()));

        verifyNoInteractions(milvusClient);
    }

    @Test
    void countByDocId_blankTenant_throwsParamError() {
        assertThatThrownBy(() -> counter.countByDocId(1L, "  "))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> assertThat(((BaseException) ex).getCode())
                        .isEqualTo(ResultCode.PARAM_ERROR.getCode()));

        verifyNoInteractions(milvusClient);
    }

    @Test
    void countByDocId_invalidTenantFormat_throwsParamError() {
        assertThatThrownBy(() -> counter.countByDocId(1L, "tenant\"injection"))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> assertThat(((BaseException) ex).getCode())
                        .isEqualTo(ResultCode.PARAM_ERROR.getCode()));

        verifyNoInteractions(milvusClient);
    }

    @Test
    void countByDocId_returnsParsedCount() {
        QueryResp.QueryResult row = QueryResp.QueryResult.builder()
                .entity(Map.of("count(*)", 7L))
                .build();
        QueryResp response = QueryResp.builder().queryResults(List.of(row)).build();
        when(milvusClient.query(any(QueryReq.class))).thenReturn(response);

        long count = counter.countByDocId(11L, "tenant-1");

        assertThat(count).isEqualTo(7L);
    }

    @Test
    void countByDocId_buildsTenantScopedFilter() {
        QueryResp response = QueryResp.builder().queryResults(List.of()).build();
        when(milvusClient.query(any(QueryReq.class))).thenReturn(response);

        counter.countByDocId(11L, "tenant-1");

        ArgumentCaptor<QueryReq> captor = ArgumentCaptor.forClass(QueryReq.class);
        verify(milvusClient).query(captor.capture());
        QueryReq req = captor.getValue();
        assertThat(req.getCollectionName()).isEqualTo("test_collection");
        assertThat(req.getFilter()).isEqualTo("doc_id == \"11\" and tenant_id == \"tenant-1\"");
    }

    @Test
    void countByDocId_emptyResults_returnsZero() {
        QueryResp response = QueryResp.builder().queryResults(List.of()).build();
        when(milvusClient.query(any(QueryReq.class))).thenReturn(response);

        assertThat(counter.countByDocId(11L, "tenant-1")).isZero();
    }

    @Test
    void countByDocId_nullResults_returnsZero() {
        QueryResp response = QueryResp.builder().queryResults(null).build();
        when(milvusClient.query(any(QueryReq.class))).thenReturn(response);

        assertThat(counter.countByDocId(11L, "tenant-1")).isZero();
    }

    @Test
    void countByDocId_stringCount_parsesNumber() {
        QueryResp.QueryResult row = QueryResp.QueryResult.builder()
                .entity(Map.of("count(*)", "12"))
                .build();
        QueryResp response = QueryResp.builder().queryResults(List.of(row)).build();
        when(milvusClient.query(any(QueryReq.class))).thenReturn(response);

        assertThat(counter.countByDocId(11L, "tenant-1")).isEqualTo(12L);
    }

    @Test
    void countByDocId_milvusError_wrappedInBaseException() {
        when(milvusClient.query(any(QueryReq.class))).thenThrow(new RuntimeException("connection refused"));

        assertThatThrownBy(() -> counter.countByDocId(11L, "tenant-1"))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> {
                    BaseException be = (BaseException) ex;
                    assertThat(be.getCode()).isEqualTo(ResultCode.INTERNAL_ERROR.getCode());
                    assertThat(be.getMessage()).contains("vector count failed");
                });
    }
}
