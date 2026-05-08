package com.schemaplexai.system.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.schemaplexai.dao.config.TenantLineInterceptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class MyBatisPlusConfigTest {

    @Mock
    private TenantLineInterceptor tenantLineInterceptor;

    @Test
    void mybatisPlusInterceptor_returnsNonNullInterceptor() {
        MyBatisPlusConfig config = new MyBatisPlusConfig(tenantLineInterceptor);

        MybatisPlusInterceptor interceptor = config.mybatisPlusInterceptor();

        assertThat(interceptor).isNotNull();
    }
}
