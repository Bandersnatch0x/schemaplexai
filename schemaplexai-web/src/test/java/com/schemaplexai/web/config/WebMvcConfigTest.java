package com.schemaplexai.web.config;

import com.schemaplexai.web.interceptor.TenantContextInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {WebMvcConfig.class, TenantContextInterceptor.class})
class WebMvcConfigTest {

    @Test
    void webMvcConfigBeanExists(ApplicationContext ctx) {
        assertThat(ctx.getBean(WebMvcConfig.class)).isNotNull();
    }

    @Test
    void tenantContextInterceptorBeanExists(ApplicationContext ctx) {
        assertThat(ctx.getBean(TenantContextInterceptor.class)).isNotNull();
    }
}
