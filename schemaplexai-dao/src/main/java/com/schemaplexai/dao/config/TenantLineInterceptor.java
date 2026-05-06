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
        return new StringValue(tenantId);
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
            || tableName.startsWith("act_");
    }
}
