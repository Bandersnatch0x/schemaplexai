package com.schemaplexai.ops.controller;

import com.schemaplexai.common.result.Result;
import com.schemaplexai.ops.entity.SfDeliveryRecord;
import com.schemaplexai.ops.service.DeliveryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryControllerTest {

    @Mock
    private DeliveryService deliveryService;

    @InjectMocks
    private DeliveryController deliveryController;

    private SfDeliveryRecord deliveryRecord;

    @BeforeEach
    void setUp() {
        deliveryRecord = new SfDeliveryRecord();
        deliveryRecord.setId(1L);
        deliveryRecord.setArtifactId(10L);
        deliveryRecord.setDeliveryType("email");
        deliveryRecord.setRecipient("user@example.com");
        deliveryRecord.setStatus(0);
    }

    @Test
    void create_returnsId() {
        when(deliveryService.save(any())).thenReturn(true);

        Result<Long> result = deliveryController.create(deliveryRecord);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEqualTo(1L);
    }

    @Test
    void update_returnsBoolean() {
        when(deliveryService.updateById(any())).thenReturn(true);

        Result<Boolean> result = deliveryController.update(1L, deliveryRecord);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isTrue();
    }

    @Test
    void delete_returnsBoolean() {
        when(deliveryService.removeById(1L)).thenReturn(true);

        Result<Boolean> result = deliveryController.delete(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isTrue();
    }

    @Test
    void get_found() {
        when(deliveryService.getById(1L)).thenReturn(deliveryRecord);

        Result<SfDeliveryRecord> result = deliveryController.get(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getRecipient()).isEqualTo("user@example.com");
    }

    @Test
    void get_notFound() {
        when(deliveryService.getById(1L)).thenReturn(null);

        Result<SfDeliveryRecord> result = deliveryController.get(1L);

        assertThat(result.getCode()).isEqualTo(404);
    }

    @Test
    void list_returnsRecords() {
        when(deliveryService.list()).thenReturn(List.of(deliveryRecord));

        Result<List<SfDeliveryRecord>> result = deliveryController.list();

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).hasSize(1);
    }

    @Test
    void createDelivery_returnsRecord() {
        when(deliveryService.createDelivery(10L, "email", "user@example.com")).thenReturn(deliveryRecord);

        Result<SfDeliveryRecord> result = deliveryController.createDelivery(10L, "email", "user@example.com");

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getDeliveryType()).isEqualTo("email");
    }

    @Test
    void trackDelivery_returnsRecord() {
        when(deliveryService.trackDelivery(1L, 1)).thenReturn(deliveryRecord);

        Result<SfDeliveryRecord> result = deliveryController.trackDelivery(1L, 1);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getId()).isEqualTo(1L);
    }

    @Test
    void confirmDelivery_returnsRecord() {
        when(deliveryService.confirmDelivery(1L)).thenReturn(deliveryRecord);

        Result<SfDeliveryRecord> result = deliveryController.confirmDelivery(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getId()).isEqualTo(1L);
    }

    @Test
    void listByStatus_returnsRecords() {
        when(deliveryService.listByStatus(0)).thenReturn(List.of(deliveryRecord));

        Result<List<SfDeliveryRecord>> result = deliveryController.listByStatus(0);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).hasSize(1);
    }
}
