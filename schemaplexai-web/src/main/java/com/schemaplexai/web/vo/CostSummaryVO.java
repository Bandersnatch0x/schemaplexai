package com.schemaplexai.web.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * M6.4: Cost summary value object for HTTP API responses.
 */
@Data
public class CostSummaryVO {

    private BigDecimal totalCost;

    private BigDecimal todayCost;

    private BigDecimal monthCost;

    private String currency;
}
