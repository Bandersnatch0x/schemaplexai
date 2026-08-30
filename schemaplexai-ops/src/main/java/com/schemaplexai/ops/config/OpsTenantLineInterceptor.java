package com.schemaplexai.ops.config;

import com.schemaplexai.dao.config.TenantLineInterceptor;

/**
 * Ops-specific tenant-line handler (review ST-01).
 *
 * <p>Extends the shared dao {@link TenantLineInterceptor} semantics (fail-closed:
 * an empty tenant context resolves to a {@code NULL} tenant expression, so queries
 * match nothing) with ignore rules for ops tables that carry no {@code tenant_id}
 * column. Without these ignores the interceptor would inject a nonexistent column
 * into their SQL:
 *
 * <ul>
 *   <li>{@code sf_sync_cursor} / {@code sf_sync_batch_log} — sync metadata written
 *       exclusively by the cross-tenant PostgreSQL→ClickHouse cost sync job
 *       (DDL {@code 03-init-schema-others.sql:354-381}, no tenant column).</li>
 * </ul>
 *
 * <p>Cross-tenant reads that remain necessary on business tables (the hourly
 * budget alert scan) are not exempted here; they use mapper methods annotated
 * with {@code @InterceptorIgnore} and documented on the method itself.
 */
public class OpsTenantLineInterceptor extends TenantLineInterceptor {

    @Override
    public boolean ignoreTable(String tableName) {
        return super.ignoreTable(tableName)
                || "sf_sync_cursor".equals(tableName)
                || "sf_sync_batch_log".equals(tableName);
    }
}
