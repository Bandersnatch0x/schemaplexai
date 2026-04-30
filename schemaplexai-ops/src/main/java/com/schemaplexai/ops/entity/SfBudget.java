package com.schemaplexai.ops.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.schemaplexai.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_budget")
public class SfBudget extends BaseEntity {

    private String budgetType;
    private BigDecimal limitAmount;
    private BigDecimal usedAmount;
    private BigDecimal alertThreshold;
}
