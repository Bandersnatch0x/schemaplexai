package com.schemaplexai.web.mapper;

import com.schemaplexai.web.vo.CostSummaryVO;
import com.schemaplexai.web.vo.ExecutionCostVO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * M6.4: Cost map to VO mapper.
 */
@Component
public class CostMapper {

    public CostSummaryVO toCostSummaryVO(Map<String, ?> source) {
        if (source == null) {
            return null;
        }
        CostSummaryVO vo = new CostSummaryVO();
        vo.setTotalCost(toBigDecimal(source.get("totalCost")));
        vo.setTodayCost(toBigDecimal(source.get("todayCost")));
        vo.setMonthCost(toBigDecimal(source.get("monthCost")));
        vo.setCurrency(toString(source.get("currency")));
        return vo;
    }

    public ExecutionCostVO toExecutionCostVO(Map<String, ?> source) {
        if (source == null) {
            return null;
        }
        ExecutionCostVO vo = new ExecutionCostVO();
        vo.setExecutionId(toLong(source.get("executionId")));
        vo.setTenantId(toLong(source.get("tenantId")));
        vo.setTotalCost(toBigDecimal(source.get("totalCost")));
        vo.setCurrency(toString(source.get("currency")));
        vo.setInputTokens(toLong(source.get("inputTokens")));
        vo.setOutputTokens(toLong(source.get("outputTokens")));
        vo.setTotalTokens(toLong(source.get("totalTokens")));
        Object recordCount = source.get("recordCount");
        vo.setRecordCount(recordCount instanceof Integer i ? i : null);
        return vo;
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        if (value instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        return null;
    }

    private static Long toLong(Object value) {
        if (value instanceof Long l) {
            return l;
        }
        if (value instanceof Integer i) {
            return i.longValue();
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        return null;
    }

    private static String toString(Object value) {
        return value instanceof String s ? s : null;
    }
}
