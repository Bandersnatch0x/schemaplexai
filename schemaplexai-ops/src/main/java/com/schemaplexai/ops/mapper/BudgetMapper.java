package com.schemaplexai.ops.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.schemaplexai.dao.mapper.BaseMapperX;
import com.schemaplexai.ops.entity.SfBudget;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface BudgetMapper extends BaseMapperX<SfBudget> {

    /**
     * Cross-tenant scan of all active budgets for the hourly budget alert job
     * (review ST-01 / cost-analytics spec §3.3).
     *
     * <p>Documented tenant-line deviation, same pattern as the task module's
     * {@code MilvusReconciliationMapper}: the alert job is an analytics operation
     * that must evaluate every tenant's budget in one pass, so this method opts
     * out of the tenant interceptor via {@code @InterceptorIgnore}. Tenant
     * isolation is re-established downstream: {@code CostService.checkBudgetAlerts}
     * re-injects each budget's tenant into {@code TenantContextHolder} before the
     * notification dedup check and insert run, and the public alert-listing
     * endpoint ({@code GET /ops/budgets/alerts}) is strictly tenant-scoped.
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("""
            SELECT id, tenant_id, budget_type, limit_amount, used_amount, alert_threshold,
                   created_at, updated_at, deleted
            FROM sf_budget
            WHERE deleted = 0
            ORDER BY id ASC
            """)
    List<SfBudget> selectAllActiveBudgetsCrossTenant();
}
