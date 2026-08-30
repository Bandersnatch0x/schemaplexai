package com.schemaplexai.web.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.schemaplexai.dao.config.TenantLineInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus plugin registration for the web runtime (issue 926).
 *
 * <p>The web application only scans {@code com.schemaplexai.web}
 * (see {@code SchemaPlexaiWebApplication}), so the {@link TenantLineInterceptor}
 * {@code @Component} living in {@code com.schemaplexai.dao.config} is never
 * instantiated here and no {@code MybatisPlusInterceptor} was registered at all.
 * As a consequence {@code selectPage} degraded into an unbounded full scan with
 * {@code total}/{@code pages} always 0, and no {@code tenant_id} condition was
 * injected into notification SQL.
 *
 * <p>Mirrors the plugin wiring used by the other business modules
 * (e.g. {@code schemaplexai-agent-config}): tenant filtering first, then
 * pagination. The handler is exposed as a bean explicitly because the dao
 * component is outside this module's scan base packages.
 */
@Configuration
public class MyBatisPlusConfig {

    @Bean
    public TenantLineInterceptor tenantLineInterceptor() {
        return new TenantLineInterceptor();
    }

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(TenantLineInterceptor tenantLineInterceptor) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(tenantLineInterceptor));
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        return interceptor;
    }
}
