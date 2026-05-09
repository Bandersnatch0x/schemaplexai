package com.schemaplexai.web.interceptor;

import com.schemaplexai.common.constants.CommonConstants;
import com.schemaplexai.common.context.TenantContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantContextInterceptorTest {

    private final TenantContextInterceptor interceptor = new TenantContextInterceptor();

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @AfterEach
    void cleanup() {
        TenantContextHolder.clear();
    }

    @Test
    void preHandle_setsTenantId_whenHeaderPresent() {
        when(request.getHeader(CommonConstants.HEADER_TENANT_ID)).thenReturn("tenant-001");

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        assertThat(TenantContextHolder.getTenantId()).isEqualTo("tenant-001");
    }

    @Test
    void preHandle_returnsTrue_whenHeaderMissing() {
        when(request.getHeader(CommonConstants.HEADER_TENANT_ID)).thenReturn(null);

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        assertThat(TenantContextHolder.getTenantId()).isNull();
    }

    @Test
    void preHandle_returnsTrue_whenHeaderEmpty() {
        when(request.getHeader(CommonConstants.HEADER_TENANT_ID)).thenReturn("");

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        assertThat(TenantContextHolder.getTenantId()).isNull();
    }

    @Test
    void afterCompletion_clearsTenantContext() {
        TenantContextHolder.setTenantId("tenant-001");

        interceptor.afterCompletion(request, response, new Object(), null);

        assertThat(TenantContextHolder.getTenantId()).isNull();
    }
}
