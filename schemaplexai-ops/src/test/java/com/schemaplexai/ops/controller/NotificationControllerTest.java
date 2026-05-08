package com.schemaplexai.ops.controller;

import com.schemaplexai.common.result.Result;
import com.schemaplexai.ops.entity.SfNotification;
import com.schemaplexai.ops.service.NotificationService;
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
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController notificationController;

    private SfNotification notification;

    @BeforeEach
    void setUp() {
        notification = new SfNotification();
        notification.setId(1L);
        notification.setUserId(100L);
        notification.setType("info");
        notification.setTitle("Test Title");
        notification.setContent("Test Content");
        notification.setStatus(0);
    }

    @Test
    void create_returnsId() {
        when(notificationService.save(any())).thenReturn(true);

        Result<Long> result = notificationController.create(notification);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEqualTo(1L);
    }

    @Test
    void update_returnsBoolean() {
        when(notificationService.updateById(any())).thenReturn(true);

        Result<Boolean> result = notificationController.update(1L, notification);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isTrue();
    }

    @Test
    void delete_returnsBoolean() {
        when(notificationService.removeById(1L)).thenReturn(true);

        Result<Boolean> result = notificationController.delete(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isTrue();
    }

    @Test
    void get_found() {
        when(notificationService.getById(1L)).thenReturn(notification);

        Result<SfNotification> result = notificationController.get(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getTitle()).isEqualTo("Test Title");
    }

    @Test
    void get_notFound() {
        when(notificationService.getById(1L)).thenReturn(null);

        Result<SfNotification> result = notificationController.get(1L);

        assertThat(result.getCode()).isEqualTo(404);
    }

    @Test
    void list_returnsNotifications() {
        when(notificationService.list()).thenReturn(List.of(notification));

        Result<List<SfNotification>> result = notificationController.list();

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).hasSize(1);
    }

    @Test
    void sendNotification_returnsNotification() {
        when(notificationService.sendNotification(100L, "info", "Test Title", "Test Content"))
                .thenReturn(notification);

        Result<SfNotification> result = notificationController.sendNotification(
                100L, "info", "Test Title", "Test Content");

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getUserId()).isEqualTo(100L);
    }

    @Test
    void markAsRead_returnsNotification() {
        when(notificationService.markAsRead(1L)).thenReturn(notification);

        Result<SfNotification> result = notificationController.markAsRead(1L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData().getId()).isEqualTo(1L);
    }

    @Test
    void listUnread_returnsNotifications() {
        when(notificationService.listUnread(100L)).thenReturn(List.of(notification));

        Result<List<SfNotification>> result = notificationController.listUnread(100L);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).hasSize(1);
    }

    @Test
    void batchMarkAsRead_returnsCount() {
        List<Long> ids = List.of(1L, 2L, 3L);
        when(notificationService.batchMarkAsRead(ids)).thenReturn(3);

        Result<Integer> result = notificationController.batchMarkAsRead(ids);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEqualTo(3);
    }

    @Test
    void batchMarkAsRead_returnsZeroForEmptyList() {
        List<Long> ids = List.of();
        when(notificationService.batchMarkAsRead(ids)).thenReturn(0);

        Result<Integer> result = notificationController.batchMarkAsRead(ids);

        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEqualTo(0);
    }
}
