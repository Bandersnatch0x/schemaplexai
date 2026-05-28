package com.schemaplexai.web.service.cost;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.ops.service.CostService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OpsCostQueryPort implements CostQueryPort {

    private final ObjectProvider<CostService> costServiceProvider;

    @Override
    public Map<String, Object> getCostSummary(String tenantId) {
        String parsedTenantId = parseTenantId(tenantId);
        Map<String, Object> summary = new LinkedHashMap<>(costService().queryCostByTenant(parsedTenantId));
        summary.put("currency", "USD");
        return summary;
    }

    @Override
    public Map<String, Object> getExecutionCost(String tenantId, Long executionId) {
        String parsedTenantId = parseTenantId(tenantId);
        Long parsedExecutionId = parseExecutionId(executionId);
        return costService().queryCostByExecution(parsedTenantId, parsedExecutionId);
    }

    private CostService costService() {
        CostService costService = costServiceProvider.getIfAvailable();
        if (costService == null) {
            throw new BaseException(ResultCode.ERROR,
                    "Cost query service is not available in web runtime");
        }
        return costService;
    }

    private String parseTenantId(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "tenantId must not be blank");
        }
        return tenantId;
    }

    private Long parseExecutionId(Long executionId) {
        if (executionId == null || executionId <= 0) {
            throw new BaseException(ResultCode.PARAM_ERROR, "executionId must be positive");
        }
        return executionId;
    }
}
