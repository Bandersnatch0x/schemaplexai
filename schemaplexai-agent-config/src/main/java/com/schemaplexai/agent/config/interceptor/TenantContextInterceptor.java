package com.schemaplexai.agent.config.interceptor;

import com.schemaplexai.common.constants.CommonConstants;
import com.schemaplexai.common.context.TenantContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Populates {@link TenantContextHolder} from the {@code X-Tenant-Id} header
 * injected by the gateway, so the MyBatis-Plus tenant interceptor can scope
 * every query of this service to the requesting tenant. Mirrors the web
 * module's {@code TenantContextInterceptor} (same validation rules).
 */
@Slf4j
@Component
public class TenantContextInterceptor implements HandlerInterceptor {

    private static final int MAX_TENANT_ID_LENGTH = 64;
    private static final java.util.regex.Pattern TENANT_ID_PATTERN = java.util.regex.Pattern.compile("^[a-zA-Z0-9_-]+$");

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String tenantId = request.getHeader(CommonConstants.HEADER_TENANT_ID);
        if (StringUtils.hasText(tenantId)) {
            if (tenantId.length() > MAX_TENANT_ID_LENGTH || !TENANT_ID_PATTERN.matcher(tenantId).matches()) {
                log.warn("Invalid tenant ID rejected: {}", tenantId.length() > MAX_TENANT_ID_LENGTH ? "(too long)" : tenantId);
                return true; // pass through without setting context — downstream will handle missing tenant
            }
            TenantContextHolder.setTenantId(tenantId);
            log.debug("Tenant context set: {}", tenantId);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        TenantContextHolder.clear();
        log.debug("Tenant context cleared");
    }
}
