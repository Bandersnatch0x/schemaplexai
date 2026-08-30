package com.schemaplexai.context.web;

import com.schemaplexai.common.constants.CommonConstants;
import com.schemaplexai.common.context.TenantContextHolder;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class TenantContextFilterTest {

    private final TenantContextFilter filter = new TenantContextFilter();

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void validHeader_setsTenantForDownstreamAndClearsAfterwards() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CommonConstants.HEADER_TENANT_ID, "tenant-1");
        AtomicReference<String> seenInChain = new AtomicReference<>();
        MockFilterChain chain = new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
                seenInChain.set(TenantContextHolder.getTenantId());
            }
        };

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(seenInChain.get()).isEqualTo("tenant-1");
        assertThat(TenantContextHolder.getTenantId()).isNull();
    }

    @Test
    void missingHeader_leavesTenantUnset() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        AtomicReference<String> seenInChain = new AtomicReference<>();
        MockFilterChain chain = new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
                seenInChain.set(TenantContextHolder.getTenantId());
            }
        };

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(seenInChain.get()).isNull();
    }

    @Test
    void invalidHeaderCharacters_isIgnoredFailClosed() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CommonConstants.HEADER_TENANT_ID, "tenant;drop");
        AtomicReference<String> seenInChain = new AtomicReference<>();
        MockFilterChain chain = new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
                seenInChain.set(TenantContextHolder.getTenantId());
            }
        };

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(seenInChain.get()).isNull();
    }

    @Test
    void oversizedHeader_isIgnored() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CommonConstants.HEADER_TENANT_ID, "t".repeat(65));
        AtomicReference<String> seenInChain = new AtomicReference<>();
        MockFilterChain chain = new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
                seenInChain.set(TenantContextHolder.getTenantId());
            }
        };

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(seenInChain.get()).isNull();
    }

    @Test
    void chainException_stillClearsTenant() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CommonConstants.HEADER_TENANT_ID, "tenant-1");
        MockFilterChain chain = new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res)
                    throws ServletException {
                throw new ServletException("boom");
            }
        };

        org.junit.jupiter.api.Assertions.assertThrows(ServletException.class,
                () -> filter.doFilter(request, new MockHttpServletResponse(), chain));

        assertThat(TenantContextHolder.getTenantId()).isNull();
    }
}
