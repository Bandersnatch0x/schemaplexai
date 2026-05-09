package com.schemaplexai.web.config;

import com.schemaplexai.web.interceptor.TenantContextInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WebMvcConfigMethodTest {

    @Test
    void addCorsMappings_registersGlobalCors() {
        TenantContextInterceptor interceptor = mock(TenantContextInterceptor.class);
        WebMvcConfig config = new WebMvcConfig(interceptor);
        CorsRegistry registry = new CorsRegistry();

        config.addCorsMappings(registry);
        // CorsRegistry does not expose getters; we verify no exception is thrown.
    }

    @Test
    void addInterceptors_registersTenantInterceptor() {
        TenantContextInterceptor interceptor = mock(TenantContextInterceptor.class);
        WebMvcConfig config = new WebMvcConfig(interceptor);

        InterceptorRegistry registry = mock(InterceptorRegistry.class);
        InterceptorRegistration registration = mock(InterceptorRegistration.class);
        when(registry.addInterceptor(any())).thenReturn(registration);
        when(registration.addPathPatterns(any(String[].class))).thenReturn(registration);
        when(registration.excludePathPatterns(any(String[].class))).thenReturn(registration);

        config.addInterceptors(registry);

        verify(registry).addInterceptor(interceptor);
    }
}
