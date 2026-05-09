package com.schemaplexai.ops.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.schemaplexai.dao.mapper.BaseMapperX;
import com.schemaplexai.ops.entity.BudgetConfig;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BudgetConfigMapper extends BaseMapperX<BudgetConfig> {

    default BudgetConfig selectByTenantId(String tenantId) {
        return selectOne(
                new LambdaQueryWrapper<BudgetConfig>()
                        .eq(BudgetConfig::getTenantId, tenantId)
                        .orderByDesc(BudgetConfig::getCreatedAt)
                        .last("LIMIT 1")
        );
    }
}
