package com.schemaplexai.web.service.notification;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.dao.mapper.notification.NotificationMapper;
import com.schemaplexai.model.entity.notification.Notification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationMapper notificationMapper;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private Notification notification;

    @BeforeEach
    void setUp() {
        notification = new Notification();
        notification.setId(1L);
        notification.setUserId(100L);
        notification.setTitle("Test Title");
        notification.setContent("Test Content");
        notification.setType("SYSTEM");
        notification.setRead(false);
    }

    @Test
    void markAsRead_shouldReturnTrue_whenNotificationExists() {
        when(notificationMapper.markAsRead(1L, 100L)).thenReturn(1);

        boolean result = notificationService.markAsRead(1L, 100L);

        assertThat(result).isTrue();
    }

    @Test
    void markAsRead_shouldThrowNotFound_whenNotificationDoesNotExist() {
        when(notificationMapper.markAsRead(999L, 100L)).thenReturn(0);

        assertThatThrownBy(() -> notificationService.markAsRead(999L, 100L))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("通知不存在");
    }

    @Test
    void markAllAsRead_shouldReturnAffectedCount() {
        when(notificationMapper.markAllAsRead(100L)).thenReturn(5);

        int result = notificationService.markAllAsRead(100L);

        assertThat(result).isEqualTo(5);
    }
}
