package com.schemaplexai.context.service.impl;

import com.schemaplexai.context.service.MilvusSyncService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

import static org.assertj.core.api.Assertions.assertThat;

class MilvusSyncServiceBeanTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(MilvusComponentScanConfig.class);

    @Test
    void milvusMissing_componentScanProvidesNoOpMilvusSyncServiceBean() {
        contextRunner
                .run(context -> assertThat(context)
                        .hasSingleBean(MilvusSyncService.class)
                        .getBean(MilvusSyncService.class)
                        .isInstanceOf(NoOpMilvusSyncServiceImpl.class));
    }

    @Configuration(proxyBeanMethods = false)
    @ComponentScan(
            basePackageClasses = NoOpMilvusSyncServiceImpl.class,
            useDefaultFilters = false,
            includeFilters = @ComponentScan.Filter(
                    type = FilterType.ASSIGNABLE_TYPE,
                    classes = {MilvusSyncServiceImpl.class, NoOpMilvusSyncServiceImpl.class}
            )
    )
    static class MilvusComponentScanConfig {
    }
}
