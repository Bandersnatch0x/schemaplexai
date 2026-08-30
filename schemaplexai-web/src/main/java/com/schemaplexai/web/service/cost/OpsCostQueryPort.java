package com.schemaplexai.web.service.cost;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.ops.service.CostService;
import com.schemaplexai.web.mapper.CostMapper;
import com.schemaplexai.web.vo.CostSummaryVO;
import com.schemaplexai.web.vo.ExecutionCostVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class OpsCostQueryPort implements CostQueryPort {

    private final ObjectProvider<CostService> costServiceProvider;
    private final CostMapper costMapper;

    @Override
    public CostSummaryVO getCostSummary(String tenantId) {
        String parsedTenantId = parseTenantId(tenantId);
        Map<String, Object> raw = new java.util.LinkedHashMap<>(costService().queryCostByTenant(parsedTenantId));
        raw.put("currency", "USD");
        return costMapper.toCostSummaryVO(raw);
    }

    @Override
    public ExecutionCostVO getExecutionCost(String tenantId, Long executionId) {
        String parsedTenantId = parseTenantId(tenantId);
        Long parsedExecutionId = parseExecutionId(executionId);
        Map<String, Object> raw = costService().queryCostByExecution(parsedTenantId, parsedExecutionId);
        return costMapper.toExecutionCostVO(raw);
    }

    private CostService costService() {
        CostService service = costServiceProvider.getIfAvailable();
        if (service == null) {
            throw new BaseException(ResultCode.ERROR,
                    "Cost query service is not available in web runtime");
        }
        return service;
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
