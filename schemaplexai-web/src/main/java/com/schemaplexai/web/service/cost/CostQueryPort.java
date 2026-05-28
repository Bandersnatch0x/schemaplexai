package com.schemaplexai.web.service.cost;

import java.util.Map;

public interface CostQueryPort {

    Map<String, Object> getCostSummary(String tenantId);

    Map<String, Object> getExecutionCost(String tenantId, Long executionId);
}
