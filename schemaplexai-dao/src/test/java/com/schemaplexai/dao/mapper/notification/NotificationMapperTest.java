package com.schemaplexai.dao.mapper.notification;

import com.schemaplexai.common.context.TenantContextHolder;
import com.schemaplexai.model.entity.notification.Notification;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = com.schemaplexai.dao.TestApplication.class)
@Sql(scripts = "/schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@Transactional
class NotificationMapperTest {

    @Autowired
    private NotificationMapper notificationMapper;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId("test-tenant");
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void insert_selectById() {
        Notification notification = new Notification();
        notification.setTenantId("test-tenant");
        notification.setUserId(1L);
        notification.setTitle("Test");
        notification.setContent("Content");
        notification.setType("SYSTEM");
        notification.setRead(false);
        notification.setDeleted(0);

        int rows = notificationMapper.insert(notification);
        assertThat(rows).isEqualTo(1);
        assertThat(notification.getId()).isNotNull();

        Notification found = notificationMapper.selectById(notification.getId());
        assertThat(found).isNotNull();
        assertThat(found.getTitle()).isEqualTo("Test");
        assertThat(found.getRead()).isFalse();
    }

    @Test
    void markAsRead_updatesSingleRow() {
        Notification n1 = new Notification();
        n1.setTenantId("test-tenant");
        n1.setUserId(1L);
        n1.setTitle("T1");
        n1.setContent("C1");
        n1.setType("SYSTEM");
        n1.setRead(false);
        n1.setDeleted(0);
        notificationMapper.insert(n1);

        Notification n2 = new Notification();
        n2.setTenantId("test-tenant");
        n2.setUserId(2L);
        n2.setTitle("T2");
        n2.setContent("C2");
        n2.setType("SYSTEM");
        n2.setRead(false);
        n2.setDeleted(0);
        notificationMapper.insert(n2);

        int affected = notificationMapper.markAsRead(n1.getId(), 1L);
        assertThat(affected).isEqualTo(1);

        Notification found = notificationMapper.selectById(n1.getId());
        assertThat(found.getRead()).isTrue();
    }

    @Test
    void markAsRead_noMatch_returnsZero() {
        int affected = notificationMapper.markAsRead(999L, 1L);
        assertThat(affected).isEqualTo(0);
    }

    @Test
    void markAllAsRead_updatesOnlyUnreadForUser() {
        Notification n1 = new Notification();
        n1.setTenantId("test-tenant");
        n1.setUserId(1L);
        n1.setTitle("T1");
        n1.setContent("C1");
        n1.setType("SYSTEM");
        n1.setRead(false);
        n1.setDeleted(0);
        notificationMapper.insert(n1);

        Notification n2 = new Notification();
        n2.setTenantId("test-tenant");
        n2.setUserId(1L);
        n2.setTitle("T2");
        n2.setContent("C2");
        n2.setType("SYSTEM");
        n2.setRead(false);
        n2.setDeleted(0);
        notificationMapper.insert(n2);

        Notification n3 = new Notification();
        n3.setTenantId("test-tenant");
        n3.setUserId(2L);
        n3.setTitle("T3");
        n3.setContent("C3");
        n3.setType("SYSTEM");
        n3.setRead(false);
        n3.setDeleted(0);
        notificationMapper.insert(n3);

        int affected = notificationMapper.markAllAsRead(1L);
        assertThat(affected).isEqualTo(2);
    }

    @Test
    void markAllAsRead_noUnread_returnsZero() {
        int affected = notificationMapper.markAllAsRead(99L);
        assertThat(affected).isEqualTo(0);
    }
}
