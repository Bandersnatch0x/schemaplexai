package com.schemaplexai.task.web;

import com.schemaplexai.common.context.TenantContextHolder;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * The task request chain must assemble the tenant (and user) context from the
 * gateway-injected headers; otherwise the tenant-line interceptor registered in
 * {@code MyBatisPlusConfig} would fail every board request closed and comment
 * authorship could not be attributed. Same pattern as the ops module filter test
 * (review ST-01).
 */
class TenantContextFilterTest {

    private final TenantContextFilter filter = new TenantContextFilter();

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
        TaskUserContextHolder.clear();
    }

    @Test
    void validHeaders_populateContextsForDownstreamAndClearAfter() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Id", "42");
        request.addHeader("X-User-Id", "7");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        AtomicReference<String> tenantSeenDownstream = new AtomicReference<>();
        AtomicReference<Long> userSeenDownstream = new AtomicReference<>();
        doAnswer(inv -> {
            tenantSeenDownstream.set(TenantContextHolder.getTenantId());
            userSeenDownstream.set(TaskUserContextHolder.getUserId());
            return null;
        }).when(chain).doFilter(any(), any());

        filter.doFilter(request, response, chain);

        assertThat(tenantSeenDownstream.get()).isEqualTo("42");
        assertThat(userSeenDownstream.get()).isEqualTo(7L);
        assertThat(TenantContextHolder.getTenantId())
                .as("tenant holder must be cleared after the request to avoid pooled-thread leakage")
                .isNull();
        assertThat(TaskUserContextHolder.getUserId())
                .as("user holder must be cleared after the request to avoid pooled-thread leakage")
                .isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    void missingHeaders_leaveContextsEmpty() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(TenantContextHolder.getTenantId()).isNull();
        assertThat(TaskUserContextHolder.getUserId()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    void invalidHeaders_areRejectedAndContextsStayEmpty() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Id", "tenant;DROP TABLE");
        request.addHeader("X-User-Id", "not-a-number");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(TenantContextHolder.getTenantId()).isNull();
        assertThat(TaskUserContextHolder.getUserId()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    void contextsAreClearedEvenWhenDownstreamThrows() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Id", "42");
        request.addHeader("X-User-Id", "7");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        doAnswer(inv -> {
            throw new IllegalStateException("boom");
        }).when(chain).doFilter(any(), any());

        try {
            filter.doFilter(request, response, chain);
        } catch (IllegalStateException expected) {
            // downstream failure must propagate
        }

        assertThat(TenantContextHolder.getTenantId()).isNull();
        assertThat(TaskUserContextHolder.getUserId()).isNull();
    }
}
