package com.schemaplexai.web.persistence;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.schemaplexai.common.context.TenantContextHolder;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.dao.mapper.notification.NotificationMapper;
import com.schemaplexai.model.entity.notification.Notification;
import com.schemaplexai.web.service.notification.NotificationService;
import com.schemaplexai.web.vo.notification.NotificationVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Issue 926 / REQ-09 / REQ-13: proves that the {@code MybatisPlusInterceptor}
 * registered by the web {@code MyBatisPlusConfig} is effective at the SQL level:
 *
 * <ul>
 *   <li>{@code selectPage} performs a real count query and applies LIMIT, so
 *       {@code total}/{@code pages} are populated (REQ-13);</li>
 *   <li>{@code TenantLineInnerInterceptor} injects the {@code tenant_id}
 *       condition, so rows of other tenants are invisible and cannot be
 *       marked as read (REQ-09, spec §6).</li>
 * </ul>
 */
@SpringBootTest(
        classes = NotificationPersistenceTestConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:web926;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password="
        })
@Sql(scripts = "/notification-schema.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Transactional
@DisplayName("926: pagination + tenant interception effective at SQL level (H2)")
class NotificationPaginationTenantIntegrationTest {

    private static final String TENANT_A = "tenant-a";
    private static final String TENANT_B = "tenant-b";

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationMapper notificationMapper;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(TENANT_A);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("REQ-13: selectPage applies LIMIT and count — total/pages populated")
    void pageQuery_paginationEffective_totalAndPagesPopulated() {
        LocalDateTime base = LocalDateTime.of(2026, 8, 1, 10, 0);
        for (int i = 1; i <= 5; i++) {
            insert(TENANT_A, 1L, "notice-" + i, base.plusMinutes(i));
        }

        IPage<NotificationVO> firstPage = notificationService.pageQuery(1L, 1, 2, null);
        assertThat(firstPage.getTotal()).isEqualTo(5);
        assertThat(firstPage.getPages()).isEqualTo(3);
        assertThat(firstPage.getCurrent()).isEqualTo(1);
        assertThat(firstPage.getSize()).isEqualTo(2);
        assertThat(firstPage.getRecords()).hasSize(2);
        // ordered by created_at desc: the two newest rows land on page 1
        assertThat(firstPage.getRecords())
                .extracting(NotificationVO::getTitle)
                .containsExactly("notice-5", "notice-4");

        IPage<NotificationVO> lastPage = notificationService.pageQuery(1L, 3, 2, null);
        assertThat(lastPage.getTotal()).isEqualTo(5);
        assertThat(lastPage.getRecords())
                .extracting(NotificationVO::getTitle)
                .containsExactly("notice-1");
    }

    @Test
    @DisplayName("REQ-09: tenant_id condition injected — other tenant rows invisible")
    void pageQuery_tenantFilter_otherTenantRowsInvisible() {
        LocalDateTime base = LocalDateTime.of(2026, 8, 1, 10, 0);
        insert(TENANT_A, 1L, "a-1", base.plusMinutes(1));
        insert(TENANT_A, 1L, "a-2", base.plusMinutes(2));
        insert(TENANT_A, 1L, "a-3", base.plusMinutes(3));
        // same user id on purpose: only tenant_id separates these rows
        insert(TENANT_B, 1L, "b-1", base.plusMinutes(4));
        insert(TENANT_B, 1L, "b-2", base.plusMinutes(5));

        TenantContextHolder.setTenantId(TENANT_A);
        IPage<NotificationVO> tenantAPage = notificationService.pageQuery(1L, 1, 20, null);
        assertThat(tenantAPage.getTotal()).isEqualTo(3);
        assertThat(tenantAPage.getRecords())
                .extracting(NotificationVO::getTitle)
                .containsExactly("a-3", "a-2", "a-1");

        TenantContextHolder.setTenantId(TENANT_B);
        IPage<NotificationVO> tenantBPage = notificationService.pageQuery(1L, 1, 20, null);
        assertThat(tenantBPage.getTotal()).isEqualTo(2);
        assertThat(tenantBPage.getRecords())
                .extracting(NotificationVO::getTitle)
                .containsExactly("b-2", "b-1");
    }

    @Test
    @DisplayName("REQ-09: cross-tenant markAsRead hits 0 rows and yields 404")
    void markAsRead_crossTenant_returns404() {
        LocalDateTime base = LocalDateTime.of(2026, 8, 1, 10, 0);
        Long tenantBNotificationId = insert(TENANT_B, 1L, "b-only", base);

        TenantContextHolder.setTenantId(TENANT_A);
        assertThatThrownBy(() -> notificationService.markAsRead(tenantBNotificationId, 1L))
                .isInstanceOf(BaseException.class)
                .extracting(e -> ((BaseException) e).getCode())
                .isEqualTo(404);
    }

    @Test
    @DisplayName("REQ-09: empty tenant context is fail-closed — matches nothing")
    void pageQuery_noTenantContext_matchesNothing() {
        insert(TENANT_A, 1L, "visible-only-with-tenant", LocalDateTime.now());

        TenantContextHolder.clear();
        IPage<NotificationVO> page = notificationService.pageQuery(1L, 1, 20, null);

        assertThat(page.getTotal()).isZero();
        assertThat(page.getRecords()).isEmpty();
    }

    /**
     * Inserts a row while the tenant context matches the row's tenant, mirroring
     * production where the gateway always injects X-Tenant-Id before any write.
     */
    private Long insert(String tenantId, long userId, String title, LocalDateTime createdAt) {
        String previous = TenantContextHolder.getTenantId();
        TenantContextHolder.setTenantId(tenantId);
        try {
            Notification notification = new Notification();
            notification.setTenantId(tenantId);
            notification.setUserId(userId);
            notification.setTitle(title);
            notification.setContent("content of " + title);
            notification.setType("SYSTEM");
            notification.setRead(false);
            notification.setDeleted(0);
            notification.setCreatedAt(createdAt);
            notificationMapper.insert(notification);
            assertThat(notification.getId()).isNotNull();
            return notification.getId();
        } finally {
            TenantContextHolder.setTenantId(previous);
        }
    }
}
