package com.schemaplexai.agent.config.interceptor;

import com.schemaplexai.common.constants.CommonConstants;
import com.schemaplexai.common.context.TenantContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TenantContextInterceptorTest {

    private final TenantContextInterceptor interceptor = new TenantContextInterceptor();
    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final HttpServletResponse response = mock(HttpServletResponse.class);

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void shouldSetTenantContextFromHeader() {
        when(request.getHeader(CommonConstants.HEADER_TENANT_ID)).thenReturn("42");

        boolean proceed = interceptor.preHandle(request, response, new Object());

        assertThat(proceed).isTrue();
        assertThat(TenantContextHolder.getTenantId()).isEqualTo("42");
    }

    @Test
    void shouldPassThroughWithoutContextWhenHeaderMissing() {
        when(request.getHeader(CommonConstants.HEADER_TENANT_ID)).thenReturn(null);

        boolean proceed = interceptor.preHandle(request, response, new Object());

        assertThat(proceed).isTrue();
        assertThat(TenantContextHolder.getTenantId()).isNull();
    }

    @Test
    void shouldRejectMalformedTenantId() {
        when(request.getHeader(CommonConstants.HEADER_TENANT_ID)).thenReturn("tenant! injection");

        boolean proceed = interceptor.preHandle(request, response, new Object());

        assertThat(proceed).isTrue();
        assertThat(TenantContextHolder.getTenantId()).isNull();
    }

    @Test
    void shouldRejectTooLongTenantId() {
        when(request.getHeader(CommonConstants.HEADER_TENANT_ID)).thenReturn("a".repeat(65));

        boolean proceed = interceptor.preHandle(request, response, new Object());

        assertThat(proceed).isTrue();
        assertThat(TenantContextHolder.getTenantId()).isNull();
    }

    @Test
    void shouldClearContextAfterCompletion() {
        TenantContextHolder.setTenantId("42");

        interceptor.afterCompletion(request, response, new Object(), null);

        assertThat(TenantContextHolder.getTenantId()).isNull();
    }
}
