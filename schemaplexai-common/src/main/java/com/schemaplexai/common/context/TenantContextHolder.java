package com.schemaplexai.common.context;

public class TenantContextHolder {
    private static final ThreadLocal<String> TENANT_CONTEXT = new ThreadLocal<>();

    public static void setTenantId(String tenantId) {
        TENANT_CONTEXT.set(tenantId);
    }

    public static String getTenantId() {
        return TENANT_CONTEXT.get();
    }

    public static void clear() {
        TENANT_CONTEXT.remove();
    }
}
