package com.schemaplexai.web.service.cost;

import com.schemaplexai.web.vo.CostSummaryVO;
import com.schemaplexai.web.vo.ExecutionCostVO;

public interface CostQueryPort {

    CostSummaryVO getCostSummary(String tenantId);

    ExecutionCostVO getExecutionCost(String tenantId, Long executionId);
}
