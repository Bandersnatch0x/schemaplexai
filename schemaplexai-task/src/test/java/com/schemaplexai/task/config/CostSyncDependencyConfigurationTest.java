package com.schemaplexai.task.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.ops.service.CostDataSyncService;
import com.schemaplexai.ops.service.CostService;
import com.schemaplexai.ops.service.DisabledCostDataSyncService;
import com.schemaplexai.quality.service.InboxDeduplicationService;
import com.schemaplexai.task.mq.CostSyncConsumer;
import com.schemaplexai.task.mq.MessageFailLogService;
import com.schemaplexai.task.scheduling.CostStatisticsJob;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class CostSyncDependencyConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withPropertyValues("clickhouse.enabled=false")
            .withBean(MessageFailLogService.class, () -> mock(MessageFailLogService.class))
            .withBean(ObjectMapper.class, ObjectMapper::new)
            .withBean(InboxDeduplicationService.class, () -> mock(InboxDeduplicationService.class))
            .withBean(CostService.class, () -> mock(CostService.class))
            .withUserConfiguration(CostSyncTaskBeans.class, DisabledCostDataSyncService.class);

    @Test
    void clickHouseDisabled_registersFallbackSyncServiceAndTaskCostBeans() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(CostDataSyncService.class);
            assertThat(context.getBean(CostDataSyncService.class))
                    .isInstanceOf(DisabledCostDataSyncService.class);
            assertThat(context).hasSingleBean(CostSyncConsumer.class);
            assertThat(context).hasSingleBean(CostStatisticsJob.class);
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class CostSyncTaskBeans {

        @Bean
        CostSyncConsumer costSyncConsumer(
                CostDataSyncService costDataSyncService,
                MessageFailLogService messageFailLogService,
                ObjectMapper objectMapper,
                InboxDeduplicationService dedupService) {
            return new CostSyncConsumer(
                    costDataSyncService,
                    messageFailLogService,
                    objectMapper,
                    dedupService);
        }

        @Bean
        CostStatisticsJob costStatisticsJob(
                CostDataSyncService costDataSyncService,
                CostService costService) {
            return new CostStatisticsJob(costDataSyncService, costService);
        }
    }
}
