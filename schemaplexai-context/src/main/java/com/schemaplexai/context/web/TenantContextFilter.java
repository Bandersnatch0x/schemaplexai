package com.schemaplexai.context.web;

import com.schemaplexai.common.constants.CommonConstants;
import com.schemaplexai.common.context.TenantContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Populates {@link TenantContextHolder} from the gateway-injected {@code X-Tenant-Id}
 * header for every request handled by the context service.
 * <p>
 * Without this filter the context service never sees a tenant (the gateway sets the
 * header, but nothing inside this service read it), which fail-closed every
 * tenant-scoped endpoint (uploads returned 400, tenant SQL filters matched nothing).
 * Invalid header values are ignored so downstream fail-closed checks keep applying.
 * The holder is always cleared after the request to avoid leakage on pooled threads.
 */
@Slf4j
@Component
public class TenantContextFilter extends OncePerRequestFilter {

    private static final int MAX_TENANT_ID_LENGTH = 64;
    private static final Pattern TENANT_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String tenantId = request.getHeader(CommonConstants.HEADER_TENANT_ID);
        boolean tenantSet = false;
        if (tenantId != null && !tenantId.isBlank()
                && tenantId.length() <= MAX_TENANT_ID_LENGTH
                && TENANT_ID_PATTERN.matcher(tenantId).matches()) {
            TenantContextHolder.setTenantId(tenantId);
            tenantSet = true;
        } else if (tenantId != null && !tenantId.isBlank()) {
            log.warn("Rejected invalid X-Tenant-Id header value (length={})", tenantId.length());
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            if (tenantSet) {
                TenantContextHolder.clear();
            }
        }
    }
}
