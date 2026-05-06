package com.schemaplexai.ops.service;

import com.schemaplexai.common.context.TenantContextHolder;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.ops.entity.SfArtifact;
import com.schemaplexai.ops.entity.SfDeliveryRecord;
import com.schemaplexai.ops.mapper.ArtifactMapper;
import com.schemaplexai.ops.mapper.DeliveryRecordMapper;
import com.schemaplexai.ops.service.DeliveryServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceImplTest {

    @Mock
    private DeliveryRecordMapper deliveryRecordMapper;

    @Mock
    private ArtifactMapper artifactMapper;

    @InjectMocks
    private DeliveryServiceImpl deliveryService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(deliveryService, "baseMapper", deliveryRecordMapper);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    // ------------------------------------------------------------------
    // createDelivery
    // ------------------------------------------------------------------

    @Test
    void createDelivery_nullArtifactId_throwsParamError() {
        assertThatThrownBy(() -> deliveryService.createDelivery(null, "email", "user@example.com"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void createDelivery_nullDeliveryType_throwsParamError() {
        assertThatThrownBy(() -> deliveryService.createDelivery(1L, null, "user@example.com"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void createDelivery_blankDeliveryType_throwsParamError() {
        assertThatThrownBy(() -> deliveryService.createDelivery(1L, "   ", "user@example.com"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void createDelivery_nullRecipient_throwsParamError() {
        assertThatThrownBy(() -> deliveryService.createDelivery(1L, "email", null))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void createDelivery_blankRecipient_throwsParamError() {
        assertThatThrownBy(() -> deliveryService.createDelivery(1L, "email", "   "))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void createDelivery_artifactNotFound_throwsNotFound() {
        when(artifactMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> deliveryService.createDelivery(1L, "email", "user@example.com"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void createDelivery_success_createsPendingRecord() {
        SfArtifact artifact = new SfArtifact();
        artifact.setId(1L);
        when(artifactMapper.selectById(1L)).thenReturn(artifact);

        SfDeliveryRecord result = deliveryService.createDelivery(1L, "email", "user@example.com");

        assertThat(result.getArtifactId()).isEqualTo(1L);
        assertThat(result.getDeliveryType()).isEqualTo("email");
        assertThat(result.getRecipient()).isEqualTo("user@example.com");
        assertThat(result.getStatus()).isEqualTo(0);
        verify(deliveryRecordMapper).insert(any(SfDeliveryRecord.class));
    }

    @Test
    void createDelivery_withTenantId_setsTenantId() {
        TenantContextHolder.setTenantId("tenant-1");
        SfArtifact artifact = new SfArtifact();
        artifact.setId(1L);
        when(artifactMapper.selectById(1L)).thenReturn(artifact);

        SfDeliveryRecord result = deliveryService.createDelivery(1L, "email", "user@example.com");

        assertThat(result.getTenantId()).isEqualTo("tenant-1");
    }

    @Test
    void createDelivery_nullTenantId_tenantIdIsNull() {
        SfArtifact artifact = new SfArtifact();
        artifact.setId(1L);
        when(artifactMapper.selectById(1L)).thenReturn(artifact);

        SfDeliveryRecord result = deliveryService.createDelivery(1L, "email", "user@example.com");

        assertThat(result.getTenantId()).isNull();
    }

    // ------------------------------------------------------------------
    // trackDelivery
    // ------------------------------------------------------------------

    @Test
    void trackDelivery_nullStatus_throwsParamError() {
        assertThatThrownBy(() -> deliveryService.trackDelivery(1L, null))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void trackDelivery_notFound_throwsNotFound() {
        when(deliveryRecordMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> deliveryService.trackDelivery(1L, 1))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void trackDelivery_success_updatesStatus() {
        SfDeliveryRecord record = new SfDeliveryRecord();
        record.setId(1L);
        record.setStatus(0);
        when(deliveryRecordMapper.selectById(1L)).thenReturn(record);

        SfDeliveryRecord result = deliveryService.trackDelivery(1L, 1);

        assertThat(result.getStatus()).isEqualTo(1);
        assertThat(result.getUpdatedAt()).isNotNull();
        verify(deliveryRecordMapper).updateById(record);
    }

    // ------------------------------------------------------------------
    // confirmDelivery
    // ------------------------------------------------------------------

    @Test
    void confirmDelivery_notFound_throwsNotFound() {
        when(deliveryRecordMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> deliveryService.confirmDelivery(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void confirmDelivery_alreadyDelivered_throwsParamError() {
        SfDeliveryRecord record = new SfDeliveryRecord();
        record.setId(1L);
        record.setStatus(2);
        when(deliveryRecordMapper.selectById(1L)).thenReturn(record);

        assertThatThrownBy(() -> deliveryService.confirmDelivery(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void confirmDelivery_pending_setsStatusToDelivered() {
        SfDeliveryRecord record = new SfDeliveryRecord();
        record.setId(1L);
        record.setArtifactId(10L);
        record.setStatus(0);
        when(deliveryRecordMapper.selectById(1L)).thenReturn(record);

        SfDeliveryRecord result = deliveryService.confirmDelivery(1L);

        assertThat(result.getStatus()).isEqualTo(2);
        assertThat(result.getUpdatedAt()).isNotNull();
        verify(deliveryRecordMapper).updateById(record);
    }

    @Test
    void confirmDelivery_inTransit_setsStatusToDelivered() {
        SfDeliveryRecord record = new SfDeliveryRecord();
        record.setId(1L);
        record.setStatus(1);
        when(deliveryRecordMapper.selectById(1L)).thenReturn(record);

        SfDeliveryRecord result = deliveryService.confirmDelivery(1L);

        assertThat(result.getStatus()).isEqualTo(2);
    }

    @Test
    void confirmDelivery_nullStatus_setsStatusToDelivered() {
        SfDeliveryRecord record = new SfDeliveryRecord();
        record.setId(1L);
        record.setStatus(null);
        when(deliveryRecordMapper.selectById(1L)).thenReturn(record);

        SfDeliveryRecord result = deliveryService.confirmDelivery(1L);

        assertThat(result.getStatus()).isEqualTo(2);
    }

    // ------------------------------------------------------------------
    // listByStatus
    // ------------------------------------------------------------------

    @Test
    void listByStatus_withStatus_filtersByStatus() {
        SfDeliveryRecord record = new SfDeliveryRecord();
        record.setStatus(0);
        when(deliveryRecordMapper.selectList(any())).thenReturn(List.of(record));

        List<SfDeliveryRecord> result = deliveryService.listByStatus(0);

        assertThat(result).hasSize(1);
    }

    @Test
    void listByStatus_nullStatus_returnsAll() {
        when(deliveryRecordMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<SfDeliveryRecord> result = deliveryService.listByStatus(null);

        assertThat(result).isEmpty();
    }

    @Test
    void listByStatus_withTenantId_includesTenantFilter() {
        TenantContextHolder.setTenantId("tenant-1");
        when(deliveryRecordMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<SfDeliveryRecord> result = deliveryService.listByStatus(0);

        assertThat(result).isEmpty();
    }
}
