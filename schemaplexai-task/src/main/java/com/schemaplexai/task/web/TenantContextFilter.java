package com.schemaplexai.task.web;

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
 * Populates {@link TenantContextHolder} (and {@link TaskUserContextHolder}) from
 * the gateway-injected {@code X-Tenant-Id} / {@code X-User-Id} headers for every
 * request handled by the task service.
 *
 * <p>Same pattern as the ops/context modules' {@code TenantContextFilter}
 * (review ST-01 / issue 922): without this filter the task service never saw a
 * tenant (the gateway sets the header, but nothing inside this service read it),
 * so the {@code TenantLineInnerInterceptor} would match nothing and no
 * request-scoped endpoint could be tenant-scoped. Invalid header values are
 * ignored so downstream fail-closed checks keep applying. Both holders are always
 * cleared after the request to avoid leakage on pooled threads.
 *
 * <p>Identity is taken from gateway headers (the gateway validates the JWT and
 * strips client-supplied identity headers); the service does not re-validate the
 * token, mirroring the other async-plane services.
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

        boolean userSet = false;
        String userId = request.getHeader(CommonConstants.HEADER_USER_ID);
        if (userId != null && !userId.isBlank()) {
            try {
                TaskUserContextHolder.setUserId(Long.valueOf(userId.trim()));
                userSet = true;
            } catch (NumberFormatException e) {
                log.warn("Rejected non-numeric X-User-Id header value");
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            if (tenantSet) {
                TenantContextHolder.clear();
            }
            if (userSet) {
                TaskUserContextHolder.clear();
            }
        }
    }
}
