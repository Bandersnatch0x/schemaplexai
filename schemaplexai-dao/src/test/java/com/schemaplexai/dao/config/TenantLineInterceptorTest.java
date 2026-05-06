package com.schemaplexai.dao.config;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.schemaplexai.common.context.TenantContextHolder;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.NullValue;
import net.sf.jsqlparser.expression.StringValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TenantLineInterceptorTest {

    private TenantLineInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new TenantLineInterceptor();
    }

    @AfterEach
    void cleanup() {
        TenantContextHolder.clear();
    }

    @Test
    void implementsTenantLineHandler() {
        assertThat(TenantLineHandler.class.isAssignableFrom(TenantLineInterceptor.class)).isTrue();
    }

    @Test
    void getTenantIdColumn_returnsTenantId() {
        assertThat(interceptor.getTenantIdColumn()).isEqualTo("tenant_id");
    }

    @Test
    void getTenantId_returnsStringValue_whenTenantSet() {
        TenantContextHolder.setTenantId("tenant-001");
        Expression expr = interceptor.getTenantId();
        assertThat(expr).isInstanceOf(StringValue.class);
        assertThat(((StringValue) expr).getValue()).isEqualTo("tenant-001");
    }

    @Test
    void getTenantId_returnsNullValue_whenTenantNotSet() {
        Expression expr = interceptor.getTenantId();
        assertThat(expr).isInstanceOf(NullValue.class);
    }

    @Test
    void getTenantId_returnsNullValue_whenTenantEmpty() {
        TenantContextHolder.setTenantId("");
        Expression expr = interceptor.getTenantId();
        assertThat(expr).isInstanceOf(NullValue.class);
    }

    @Test
    void ignoreTable_returnsTrue_forSfTenant() {
        assertThat(interceptor.ignoreTable("sf_tenant")).isTrue();
    }

    @Test
    void ignoreTable_returnsTrue_forSfTenantEnvironmentConfig() {
        assertThat(interceptor.ignoreTable("sf_tenant_environment_config")).isTrue();
    }

    @Test
    void ignoreTable_returnsTrue_forActPrefix() {
        assertThat(interceptor.ignoreTable("act_ru_execution")).isTrue();
        assertThat(interceptor.ignoreTable("act_re_procdef")).isTrue();
    }

    @Test
    void ignoreTable_returnsFalse_forRegularTable() {
        assertThat(interceptor.ignoreTable("sf_user")).isFalse();
        assertThat(interceptor.ignoreTable("sf_agent")).isFalse();
        assertThat(interceptor.ignoreTable("sf_workflow")).isFalse();
    }
}
