package com.schemaplexai.ops.service;

import com.schemaplexai.common.context.TenantContextHolder;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.ops.entity.SfNotification;
import com.schemaplexai.ops.mapper.NotificationMapper;
import com.schemaplexai.ops.service.NotificationServiceImpl;
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
class NotificationServiceImplTest {

    @Mock
    private NotificationMapper notificationMapper;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(notificationService, "baseMapper", notificationMapper);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    // ------------------------------------------------------------------
    // sendNotification
    // ------------------------------------------------------------------

    @Test
    void sendNotification_nullUserId_throwsParamError() {
        assertThatThrownBy(() -> notificationService.sendNotification(null, "info", "Title", "Content"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void sendNotification_nullType_throwsParamError() {
        assertThatThrownBy(() -> notificationService.sendNotification(1L, null, "Title", "Content"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void sendNotification_blankType_throwsParamError() {
        assertThatThrownBy(() -> notificationService.sendNotification(1L, "   ", "Title", "Content"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void sendNotification_nullTitle_throwsParamError() {
        assertThatThrownBy(() -> notificationService.sendNotification(1L, "info", null, "Content"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void sendNotification_blankTitle_throwsParamError() {
        assertThatThrownBy(() -> notificationService.sendNotification(1L, "info", "   ", "Content"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void sendNotification_success_createsUnreadNotification() {
        SfNotification result = notificationService.sendNotification(1L, "info", "Hello", "World");

        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getType()).isEqualTo("info");
        assertThat(result.getTitle()).isEqualTo("Hello");
        assertThat(result.getContent()).isEqualTo("World");
        assertThat(result.getStatus()).isEqualTo(0);
        verify(notificationMapper).insert(any(SfNotification.class));
    }

    @Test
    void sendNotification_nullContent_allowsNullContent() {
        SfNotification result = notificationService.sendNotification(1L, "info", "Title", null);

        assertThat(result.getContent()).isNull();
    }

    @Test
    void sendNotification_withTenantId_setsTenantId() {
        TenantContextHolder.setTenantId("tenant-1");

        SfNotification result = notificationService.sendNotification(1L, "info", "Title", "Content");

        assertThat(result.getTenantId()).isEqualTo("tenant-1");
    }

    @Test
    void sendNotification_nullTenantId_tenantIdIsNull() {
        SfNotification result = notificationService.sendNotification(1L, "info", "Title", "Content");

        assertThat(result.getTenantId()).isNull();
    }

    // ------------------------------------------------------------------
    // markAsRead
    // ------------------------------------------------------------------

    @Test
    void markAsRead_notFound_throwsNotFound() {
        when(notificationMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> notificationService.markAsRead(1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.NOT_FOUND.getCode());
    }

    @Test
    void markAsRead_alreadyRead_returnsWithoutUpdate() {
        SfNotification notification = new SfNotification();
        notification.setId(1L);
        notification.setStatus(1);
        when(notificationMapper.selectById(1L)).thenReturn(notification);

        SfNotification result = notificationService.markAsRead(1L);

        assertThat(result.getStatus()).isEqualTo(1);
        verify(notificationMapper, never()).updateById(any(SfNotification.class));
    }

    @Test
    void markAsRead_unread_setsStatusToRead() {
        SfNotification notification = new SfNotification();
        notification.setId(1L);
        notification.setUserId(10L);
        notification.setStatus(0);
        when(notificationMapper.selectById(1L)).thenReturn(notification);

        SfNotification result = notificationService.markAsRead(1L);

        assertThat(result.getStatus()).isEqualTo(1);
        assertThat(result.getUpdatedAt()).isNotNull();
        verify(notificationMapper).updateById(notification);
    }

    @Test
    void markAsRead_nullStatus_setsStatusToRead() {
        SfNotification notification = new SfNotification();
        notification.setId(1L);
        notification.setStatus(null);
        when(notificationMapper.selectById(1L)).thenReturn(notification);

        SfNotification result = notificationService.markAsRead(1L);

        assertThat(result.getStatus()).isEqualTo(1);
        verify(notificationMapper).updateById(notification);
    }

    // ------------------------------------------------------------------
    // listUnread
    // ------------------------------------------------------------------

    @Test
    void listUnread_nullUserId_throwsParamError() {
        assertThatThrownBy(() -> notificationService.listUnread(null))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void listUnread_returnsUnreadNotifications() {
        SfNotification n1 = new SfNotification();
        n1.setStatus(0);
        when(notificationMapper.selectList(any())).thenReturn(List.of(n1));

        List<SfNotification> result = notificationService.listUnread(1L);

        assertThat(result).hasSize(1);
    }

    @Test
    void listUnread_withTenantId_includesTenantFilter() {
        TenantContextHolder.setTenantId("tenant-1");
        when(notificationMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<SfNotification> result = notificationService.listUnread(1L);

        assertThat(result).isEmpty();
    }

    @Test
    void listUnread_noUnread_returnsEmpty() {
        when(notificationMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<SfNotification> result = notificationService.listUnread(1L);

        assertThat(result).isEmpty();
    }

    // ------------------------------------------------------------------
    // batchMarkAsRead
    // ------------------------------------------------------------------

    @Test
    void batchMarkAsRead_nullList_returnsZero() {
        int result = notificationService.batchMarkAsRead(null);

        assertThat(result).isEqualTo(0);
    }

    @Test
    void batchMarkAsRead_emptyList_returnsZero() {
        int result = notificationService.batchMarkAsRead(Collections.emptyList());

        assertThat(result).isEqualTo(0);
    }

    @Test
    void batchMarkAsRead_mixedStatuses_countsOnlyUpdated() {
        SfNotification unread = new SfNotification();
        unread.setId(1L);
        unread.setStatus(0);
        SfNotification read = new SfNotification();
        read.setId(2L);
        read.setStatus(1);
        SfNotification nullStatus = new SfNotification();
        nullStatus.setId(3L);
        nullStatus.setStatus(null);
        when(notificationMapper.selectById(1L)).thenReturn(unread);
        when(notificationMapper.selectById(2L)).thenReturn(read);
        when(notificationMapper.selectById(3L)).thenReturn(nullStatus);

        int result = notificationService.batchMarkAsRead(List.of(1L, 2L, 3L));

        assertThat(result).isEqualTo(2);
        verify(notificationMapper).updateById(unread);
        verify(notificationMapper, never()).updateById(read);
        verify(notificationMapper).updateById(nullStatus);
    }

    @Test
    void batchMarkAsRead_notFound_skips() {
        when(notificationMapper.selectById(1L)).thenReturn(null);

        int result = notificationService.batchMarkAsRead(List.of(1L));

        assertThat(result).isEqualTo(0);
    }

    @Test
    void batchMarkAsRead_allAlreadyRead_returnsZero() {
        SfNotification read = new SfNotification();
        read.setId(1L);
        read.setStatus(1);
        when(notificationMapper.selectById(1L)).thenReturn(read);

        int result = notificationService.batchMarkAsRead(List.of(1L));

        assertThat(result).isEqualTo(0);
        verify(notificationMapper, never()).updateById(any(SfNotification.class));
    }
}
