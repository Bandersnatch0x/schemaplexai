package com.schemaplexai.system.aspect;

import com.schemaplexai.common.constants.CommonConstants;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.system.annotation.RequirePermission;
import com.schemaplexai.system.security.PermissionEvaluator;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

/**
 * Aspect that enforces {@link RequirePermission} annotations on controller methods.
 * <p>
 * Extracts the user ID from the {@code X-User-Id} request header (set by the gateway
 * after JWT validation) and checks the permission via {@link PermissionEvaluator}.
 * Throws a 403 FORBIDDEN error if the user lacks the required permission.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class PermissionAspect {

    private final PermissionEvaluator permissionEvaluator;

    @Before("@annotation(com.schemaplexai.system.annotation.RequirePermission) || @within(com.schemaplexai.system.annotation.RequirePermission)")
    public void checkPermission(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Class<?> targetClass = joinPoint.getTarget().getClass();

        // Method-level annotation takes precedence over class-level
        RequirePermission annotation = method.getAnnotation(RequirePermission.class);
        if (annotation == null) {
            // Fall back to class-level
            annotation = targetClass.getAnnotation(RequirePermission.class);
        }

        if (annotation == null) {
            // Should not happen given the pointcut, but guard anyway
            return;
        }

        // Extract user ID from the request header set by the gateway
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            log.warn("No request attributes available for permission check");
            throw new BaseException(ResultCode.UNAUTHORIZED, "No request context");
        }

        HttpServletRequest request = attrs.getRequest();
        String userIdStr = request.getHeader(CommonConstants.HEADER_USER_ID);
        if (!StringUtils.hasText(userIdStr)) {
            log.warn("X-User-Id header is missing, denying access");
            throw new BaseException(ResultCode.UNAUTHORIZED, "User not authenticated");
        }

        Long userId;
        try {
            userId = Long.parseLong(userIdStr);
        } catch (NumberFormatException e) {
            log.warn("Invalid X-User-Id header value: {}", userIdStr);
            throw new BaseException(ResultCode.UNAUTHORIZED, "Invalid user ID");
        }

        String requiredPermission = annotation.value();
        if (!permissionEvaluator.hasPermission(userId, requiredPermission)) {
            log.warn("Access denied: userId={}, requiredPermission={}", userId, requiredPermission);
            throw new BaseException(ResultCode.FORBIDDEN, "Insufficient permissions: " + requiredPermission);
        }

        log.debug("Permission granted: userId={}, permission={}", userId, requiredPermission);
    }
}
