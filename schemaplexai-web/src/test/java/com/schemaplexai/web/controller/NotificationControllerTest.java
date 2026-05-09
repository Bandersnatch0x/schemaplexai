package com.schemaplexai.web.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.schemaplexai.common.result.Result;
import com.schemaplexai.web.controller.notification.NotificationController;
import com.schemaplexai.web.service.notification.NotificationService;
import com.schemaplexai.web.vo.notification.NotificationVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController controller;

    @Test
    void page() {
        IPage<NotificationVO> page = new Page<>();
        when(notificationService.pageQuery(100L, 1, 20, null)).thenReturn(page);
        Result<IPage<NotificationVO>> result = controller.page("100", 1, 20, null);
        assertThat(result.getCode()).isEqualTo(200);
    }

    @Test
    void markAsRead() {
        when(notificationService.markAsRead(1L, 100L)).thenReturn(true);
        Result<Boolean> result = controller.markAsRead("100", 1L);
        assertThat(result.getData()).isTrue();
    }

    @Test
    void markAllAsRead() {
        when(notificationService.markAllAsRead(100L)).thenReturn(5);
        Result<Integer> result = controller.markAllAsRead("100");
        assertThat(result.getData()).isEqualTo(5);
    }
}
