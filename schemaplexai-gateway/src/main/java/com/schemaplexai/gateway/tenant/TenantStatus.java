package com.schemaplexai.gateway.tenant;

/**
 * Result of the gateway-side tenant existence/status validation (issue 913,
 * spec §4.3 "验证租户存在性（缓存查询）").
 */
public enum TenantStatus {

    /** Tenant exists and is enabled — request may proceed. */
    ACTIVE,

    /** Tenant exists but is disabled — reject at the edge. */
    DISABLED,

    /** No such tenant is known — reject at the edge (forged/stale id). */
    NOT_FOUND
}
