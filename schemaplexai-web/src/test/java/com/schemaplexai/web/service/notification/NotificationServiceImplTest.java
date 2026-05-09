package com.schemaplexai.web.service.notification;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.dao.mapper.notification.NotificationMapper;
import com.schemaplexai.model.entity.notification.Notification;
import com.schemaplexai.web.vo.notification.NotificationVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationMapper notificationMapper;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @BeforeEach
    void setUp() {
        // No additional setup needed
    }

    @Test
    void pageQuery_returnsPage() {
        Page<Notification> entityPage = new Page<>();
        entityPage.setRecords(java.util.List.of());
        entityPage.setTotal(0);
        when(notificationMapper.selectPage(any(), any())).thenReturn(entityPage);

        IPage<NotificationVO> result = notificationService.pageQuery(1L, 1, 20, null);

        assertThat(result).isNotNull();
        assertThat(result.getTotal()).isEqualTo(0);
    }

    @Test
    void pageQuery_withReadFilter() {
        Page<Notification> entityPage = new Page<>();
        Notification n = new Notification();
        n.setId(1L);
        n.setUserId(1L);
        n.setTitle("T");
        n.setContent("C");
        n.setType("SYSTEM");
        n.setRead(false);
        entityPage.setRecords(java.util.List.of(n));
        entityPage.setTotal(1);
        when(notificationMapper.selectPage(any(), any())).thenReturn(entityPage);

        IPage<NotificationVO> result = notificationService.pageQuery(1L, 1, 20, false);

        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getRecords().get(0).getTitle()).isEqualTo("T");
    }

    @Test
    void sendNotification_setsReadFalseAndInserts() {
        Notification notification = new Notification();
        notification.setTitle("Test");
        when(notificationMapper.insert(any())).thenReturn(1);

        notificationService.sendNotification(notification);

        assertThat(notification.getRead()).isFalse();
    }
}
