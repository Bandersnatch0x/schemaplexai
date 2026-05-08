package com.schemaplexai.task.mq.filter;

import com.schemaplexai.common.context.TenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

/**
 * AOP advice that wraps MQ consumer invocations and ensures
 * {@link TenantContextHolder} is cleared after each message,
 * preventing tenant context leakage across messages on pooled consumer threads.
 */
@Slf4j
public class TenantContextCleanupAdvice implements MethodInterceptor {

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        try {
            return invocation.proceed();
        } finally {
            TenantContextHolder.clear();
            log.debug("[TenantContextCleanupAdvice] Cleared tenant context after message processing");
        }
    }
}
