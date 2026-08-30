package com.schemaplexai.task.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.schemaplexai.dao.config.TenantLineInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus plugin registration for the task runtime.
 *
 * <p>The task module previously registered no {@code MybatisPlusInterceptor}:
 * tenant conditions were never injected into task SQL and {@code selectPage}
 * degraded into unbounded scans. With the new task board REST layer
 * ({@code /task/tasks}, {@code /task/jobs}) every query must be tenant-scoped,
 * so the shared dao {@link TenantLineInterceptor} handler is wired here exactly
 * like the other business modules (e.g. {@code schemaplexai-spec}): tenant
 * filtering first, then pagination.
 */
@Configuration
@RequiredArgsConstructor
public class MyBatisPlusConfig {

    private final TenantLineInterceptor tenantLineInterceptor;

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(tenantLineInterceptor));
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        return interceptor;
    }
}
