package com.schemaplexai.web.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.schemaplexai.dao.config.TenantLineInterceptor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Issue 926: the web runtime must register both the tenant-line and the
 * pagination inner interceptors, otherwise selectPage returns unbounded
 * result sets with total/pages stuck at 0 and no tenant_id filtering.
 */
class MyBatisPlusConfigTest {

    private final MyBatisPlusConfig config = new MyBatisPlusConfig();

    @Test
    void tenantLineInterceptor_exposesDaoHandlerBean() {
        TenantLineInterceptor handler = config.tenantLineInterceptor();

        assertThat(handler).isNotNull();
        assertThat(handler.getTenantIdColumn()).isEqualTo("tenant_id");
    }

    @Test
    void mybatisPlusInterceptor_registersTenantBeforePagination() {
        MybatisPlusInterceptor interceptor = config.mybatisPlusInterceptor(config.tenantLineInterceptor());

        assertThat(interceptor).isNotNull();
        List<InnerInterceptor> inner = interceptor.getInterceptors();
        assertThat(inner).hasSize(2);
        assertThat(inner.get(0)).isInstanceOf(TenantLineInnerInterceptor.class);
        assertThat(inner.get(1)).isInstanceOf(PaginationInnerInterceptor.class);
    }
}
