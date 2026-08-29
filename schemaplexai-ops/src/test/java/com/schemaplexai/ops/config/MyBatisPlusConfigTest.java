package com.schemaplexai.ops.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.schemaplexai.common.context.TenantContextHolder;
import net.sf.jsqlparser.expression.NullValue;
import net.sf.jsqlparser.expression.StringValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Review ST-01: the ops runtime must register both the tenant-line and the
 * pagination inner interceptors. Without them the {@code tenant_id} condition is
 * never injected into ops SQL (cross-tenant reads possible) and {@code selectPage}
 * degrades into an unbounded scan.
 */
class MyBatisPlusConfigTest {

    private final MyBatisPlusConfig config = new MyBatisPlusConfig();

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void mybatisPlusInterceptor_registersTenantBeforePagination() {
        MybatisPlusInterceptor interceptor = config.mybatisPlusInterceptor(config.opsTenantLineInterceptor());

        assertThat(interceptor).isNotNull();
        List<InnerInterceptor> inner = interceptor.getInterceptors();
        assertThat(inner).hasSize(2);
        assertThat(inner.get(0)).isInstanceOf(TenantLineInnerInterceptor.class);
        assertThat(inner.get(1)).isInstanceOf(PaginationInnerInterceptor.class);
    }

    @Test
    void tenantExpression_isContextTenantWhenPresent() {
        OpsTenantLineInterceptor handler = config.opsTenantLineInterceptor();
        TenantContextHolder.setTenantId("tenant-42");

        assertThat(handler.getTenantId()).isInstanceOf(StringValue.class)
                .extracting(e -> ((StringValue) e).getValue())
                .isEqualTo("tenant-42");
        assertThat(handler.getTenantIdColumn()).isEqualTo("tenant_id");
    }

    @Test
    void tenantExpression_failsClosedWithoutContext() {
        OpsTenantLineInterceptor handler = config.opsTenantLineInterceptor();
        TenantContextHolder.clear();

        // NullValue renders as "tenant_id = NULL", which matches no row: request
        // paths without a tenant context return nothing instead of cross-tenant data.
        assertThat(handler.getTenantId()).isInstanceOf(NullValue.class);
    }

    @Test
    void syncMetadataTables_areIgnoredBecauseTheyHaveNoTenantColumn() {
        OpsTenantLineInterceptor handler = config.opsTenantLineInterceptor();

        assertThat(handler.ignoreTable("sf_sync_cursor")).isTrue();
        assertThat(handler.ignoreTable("sf_sync_batch_log")).isTrue();
    }

    @Test
    void tenantScopedTables_areNotIgnored() {
        OpsTenantLineInterceptor handler = config.opsTenantLineInterceptor();

        assertThat(handler.ignoreTable("sf_budget")).isFalse();
        assertThat(handler.ignoreTable("sf_notification")).isFalse();
        assertThat(handler.ignoreTable("sf_cost_record")).isFalse();
    }

    @Test
    void globalIgnoreTables_fromSharedDaoHandler_stillApply() {
        OpsTenantLineInterceptor handler = config.opsTenantLineInterceptor();

        assertThat(handler.ignoreTable("sf_tenant")).isTrue();
        assertThat(handler.ignoreTable("act_ru_task")).isTrue();
    }
}
