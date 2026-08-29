package com.schemaplexai.ops.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus plugin registration for the ops runtime (review ST-01).
 *
 * <p>The ops module historically registered no {@code MybatisPlusInterceptor} at
 * all: although the application scans {@code com.schemaplexai.dao} (so the shared
 * {@code TenantLineInterceptor} handler bean exists), no interceptor chain consumed
 * it, so {@code tenant_id} conditions were never injected into ops SQL and
 * {@code selectPage} degraded into unbounded scans. Request-scoped endpoints such
 * as {@code GET /ops/budgets/alerts} were therefore able to return cross-tenant
 * data.
 *
 * <p>Mirrors the plugin wiring used by the other business modules
 * (e.g. {@code schemaplexai-agent-config}): tenant filtering first, then
 * pagination. The handler is the ops-specific {@link OpsTenantLineInterceptor}
 * because two ops sync-metadata tables carry no {@code tenant_id} column and must
 * be exempted from tenant injection.
 */
@Configuration
public class MyBatisPlusConfig {

    @Bean
    public OpsTenantLineInterceptor opsTenantLineInterceptor() {
        return new OpsTenantLineInterceptor();
    }

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(OpsTenantLineInterceptor opsTenantLineInterceptor) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(opsTenantLineInterceptor));
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        return interceptor;
    }
}
