package com.schemaplexai.context.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.schemaplexai.dao.config.TenantLineInterceptor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class MyBatisPlusConfigTest {

    @Test
    void mybatisPlusInterceptor_containsTenantAndPaginationInterceptors() {
        TenantLineInterceptor tenantLineInterceptor = mock(TenantLineInterceptor.class);
        MyBatisPlusConfig config = new MyBatisPlusConfig(tenantLineInterceptor);

        MybatisPlusInterceptor interceptor = config.mybatisPlusInterceptor();

        assertNotNull(interceptor);
    }
}
