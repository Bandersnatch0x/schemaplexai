package com.schemaplexai.spec.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.schemaplexai.dao.config.TenantLineInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class MyBatisPlusConfig {

    private final TenantLineInterceptor tenantLineInterceptor;

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // Recommended MyBatis-Plus order: tenant line -> pagination -> optimistic lock.
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(tenantLineInterceptor));
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        // Enforces SfSpec.@Version: UPDATE ... WHERE version = ? and auto-increment
        // (spec-management §7 concurrent-edit optimistic lock, REQ-21).
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        return interceptor;
    }
}
