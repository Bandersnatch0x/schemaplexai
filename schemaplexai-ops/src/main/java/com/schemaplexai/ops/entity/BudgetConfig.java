package com.schemaplexai.ops.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.schemaplexai.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_budget_config")
public class BudgetConfig extends BaseEntity {

    private String tenantId;
    private BigDecimal monthlyLimit;
    private String currency;
}
