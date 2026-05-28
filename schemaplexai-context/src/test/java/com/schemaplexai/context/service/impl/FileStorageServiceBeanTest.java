package com.schemaplexai.context.service.impl;

import com.schemaplexai.context.service.FileStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

class FileStorageServiceBeanTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(StorageBeanConfig.class);

    private final ApplicationContextRunner scannedContextRunner = new ApplicationContextRunner()
            .withUserConfiguration(StorageComponentScanConfig.class);

    @Test
    void minioDisabled_stillProvidesFileStorageServiceBean() {
        contextRunner
                .withPropertyValues("minio.enabled=false")
                .run(context -> assertThat(context).hasSingleBean(FileStorageService.class));
    }

    @Test
    void minioMissing_componentScanProvidesDisabledStorageBean() {
        scannedContextRunner
                .run(context -> assertThat(context)
                        .hasSingleBean(FileStorageService.class)
                        .getBean(FileStorageService.class)
                        .isInstanceOf(DisabledFileStorageService.class));
    }

    @Configuration(proxyBeanMethods = false)
    @Import({MinioFileStorageService.class, DisabledFileStorageService.class})
    static class StorageBeanConfig {
    }

    @Configuration(proxyBeanMethods = false)
    @ComponentScan(
            basePackageClasses = DisabledFileStorageService.class,
            useDefaultFilters = false,
            includeFilters = @ComponentScan.Filter(
                    type = FilterType.ASSIGNABLE_TYPE,
                    classes = {MinioFileStorageService.class, DisabledFileStorageService.class}
            )
    )
    static class StorageComponentScanConfig {
    }
}
