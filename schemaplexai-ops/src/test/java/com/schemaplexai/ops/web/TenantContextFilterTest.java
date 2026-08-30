package com.schemaplexai.ops.web;

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
 * Review ST-01: the ops request chain must assemble the tenant context from the
 * gateway-injected {@code X-Tenant-Id} header (same pattern as the context module
 * filter from issue 922); otherwise the newly registered tenant interceptor would
 * fail every request closed and no endpoint could be tenant-scoped.
 */
class TenantContextFilterTest {

    private final TenantContextFilter filter = new TenantContextFilter();

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void validTenantHeader_populatesContextForDownstreamAndClearsAfter() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Id", "tenant-1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        AtomicReference<String> tenantSeenDownstream = new AtomicReference<>();
        doAnswer(inv -> {
            tenantSeenDownstream.set(TenantContextHolder.getTenantId());
            return null;
        }).when(chain).doFilter(any(), any());

        filter.doFilter(request, response, chain);

        assertThat(tenantSeenDownstream.get()).isEqualTo("tenant-1");
        assertThat(TenantContextHolder.getTenantId())
                .as("holder must be cleared after the request to avoid pooled-thread leakage")
                .isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    void missingTenantHeader_leavesContextEmpty() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(TenantContextHolder.getTenantId()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    void invalidTenantHeader_isRejectedAndContextStaysEmpty() throws Exception {
        for (String invalid : new String[]{"tenant;DROP TABLE", "tenant id", "a".repeat(65)}) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("X-Tenant-Id", invalid);
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain chain = mock(FilterChain.class);

            filter.doFilter(request, response, chain);

            assertThat(TenantContextHolder.getTenantId())
                    .as("invalid header value must not populate the context: %s", invalid)
                    .isNull();
            verify(chain).doFilter(request, response);
        }
    }

    @Test
    void contextIsClearedEvenWhenDownstreamThrows() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-Id", "tenant-1");
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
    }
}
