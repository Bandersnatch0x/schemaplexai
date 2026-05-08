package com.schemaplexai.task.mq.filter;

import com.schemaplexai.common.context.TenantContextHolder;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TenantContextCleanupAdviceTest {

    private final TenantContextCleanupAdvice advice = new TenantContextCleanupAdvice();

    @AfterEach
    void cleanup() {
        TenantContextHolder.clear();
    }

    @Test
    void invoke_clearsTenantContextAfterSuccess() throws Throwable {
        TenantContextHolder.setTenantId("tenant-abc");

        MethodInvocation invocation = mock(MethodInvocation.class);
        when(invocation.proceed()).thenReturn("result");

        Object result = advice.invoke(invocation);

        assertEquals("result", result);
        assertNull(TenantContextHolder.getTenantId());
    }

    @Test
    void invoke_clearsTenantContextAfterException() throws Throwable {
        TenantContextHolder.setTenantId("tenant-abc");

        MethodInvocation invocation = mock(MethodInvocation.class);
        when(invocation.proceed()).thenThrow(new RuntimeException("boom"));

        assertThrows(RuntimeException.class, () -> advice.invoke(invocation));
        assertNull(TenantContextHolder.getTenantId());
    }

    @Test
    void invoke_clearsEvenWhenContextWasNull() throws Throwable {
        assertNull(TenantContextHolder.getTenantId());

        MethodInvocation invocation = mock(MethodInvocation.class);
        when(invocation.proceed()).thenReturn(null);

        advice.invoke(invocation);

        assertNull(TenantContextHolder.getTenantId());
    }
}
