package com.schemaplexai.agent.config.config;

import com.schemaplexai.agent.config.interceptor.TenantContextInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers the tenant-context interceptor for all API paths (issue 927):
 * the MyBatis-Plus tenant filter reads {@code TenantContextHolder}, which is
 * populated from the gateway-injected {@code X-Tenant-Id} header.
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final TenantContextInterceptor tenantContextInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tenantContextInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/v3/api-docs/**", "/swagger-ui/**", "/webjars/**", "/doc.html");
    }
}
