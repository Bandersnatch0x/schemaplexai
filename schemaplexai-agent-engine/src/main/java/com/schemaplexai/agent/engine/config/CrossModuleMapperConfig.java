package com.schemaplexai.agent.engine.config;

import com.schemaplexai.dao.mapper.TenantEnvironmentConfigMapper;
import com.schemaplexai.integration.mapper.McpServerMapper;
import com.schemaplexai.ops.mapper.BudgetConfigMapper;
import com.schemaplexai.ops.mapper.SfCostRecordMapper;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CrossModuleMapperConfig {

    @Bean
    public MapperFactoryBean<TenantEnvironmentConfigMapper> tenantEnvironmentConfigMapper(
            SqlSessionFactory sqlSessionFactory) {
        return mapperFactoryBean(TenantEnvironmentConfigMapper.class, sqlSessionFactory);
    }

    @Bean
    public MapperFactoryBean<McpServerMapper> mcpServerMapper(SqlSessionFactory sqlSessionFactory) {
        return mapperFactoryBean(McpServerMapper.class, sqlSessionFactory);
    }

    @Bean
    public MapperFactoryBean<BudgetConfigMapper> budgetConfigMapper(SqlSessionFactory sqlSessionFactory) {
        return mapperFactoryBean(BudgetConfigMapper.class, sqlSessionFactory);
    }

    @Bean
    public MapperFactoryBean<SfCostRecordMapper> sfCostRecordMapper(SqlSessionFactory sqlSessionFactory) {
        return mapperFactoryBean(SfCostRecordMapper.class, sqlSessionFactory);
    }

    private static <T> MapperFactoryBean<T> mapperFactoryBean(
            Class<T> mapperInterface,
            SqlSessionFactory sqlSessionFactory) {
        MapperFactoryBean<T> factoryBean = new MapperFactoryBean<>(mapperInterface);
        factoryBean.setSqlSessionFactory(sqlSessionFactory);
        return factoryBean;
    }
}
