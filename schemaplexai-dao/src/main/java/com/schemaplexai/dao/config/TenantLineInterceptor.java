package com.schemaplexai.dao.config;

import com.schemaplexai.common.context.TenantContextHolder;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.NullValue;
import org.springframework.stereotype.Component;

@Component
public class TenantLineInterceptor implements TenantLineHandler {

    @Override
    public Expression getTenantId() {
        String tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null || tenantId.isEmpty()) {
            return new NullValue();
        }
        // Production tenant columns are BIGINT — a string literal would fail with
        // "operator does not exist: bigint = character varying". Numeric tenant
        // ids are emitted as numbers; non-numeric (legacy/dev VARCHAR tables)
        // stay strings. Live defect found in browser verification round 2.
        try {
            return new net.sf.jsqlparser.expression.LongValue(Long.parseLong(tenantId));
        } catch (NumberFormatException e) {
            return new StringValue(tenantId);
        }
    }

    @Override
    public String getTenantIdColumn() {
        return "tenant_id";
    }

    @Override
    public boolean ignoreTable(String tableName) {
        // 全局表不进行租户过滤
        return tableName.equals("sf_tenant")
            || tableName.equals("sf_tenant_environment_config")
            // sf_user is queried before a tenant context exists (login/register);
            // those queries scope tenant_id explicitly. Admin listings are
            // cross-tenant by design.
            || tableName.equals("sf_user")
            || tableName.startsWith("act_");
    }
}
