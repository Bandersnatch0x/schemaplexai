package com.schemaplexai.ops.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.schemaplexai.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * Read-only billing projection of the {@code sf_ai_model} table.
 * <p>
 * The cost-analytics spec (§3.1) sources per-1K token prices from the
 * {@code sf_ai_model} extension columns {@code input_price_per_1k} /
 * {@code output_price_per_1k} / {@code currency}. The ops module owns no
 * write path for AI models (that belongs to the system module), so this
 * entity maps only the columns the billing calculation needs.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_ai_model")
public class AiModelPrice extends BaseEntity {

    private String name;

    private String modelCode;

    @TableField("input_price_per_1k")
    private BigDecimal inputPricePer1k;

    @TableField("output_price_per_1k")
    private BigDecimal outputPricePer1k;

    private String currency;
}
