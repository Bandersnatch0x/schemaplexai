package com.schemaplexai.agent.engine.integration;

import com.schemaplexai.ops.service.CostService;
import com.schemaplexai.quality.service.QualityOrchestrator;
import com.schemaplexai.workflow.service.WorkflowInstanceService;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestServiceConfig {

    @Bean
    @Primary
    public WorkflowInstanceService workflowInstanceService() {
        return Mockito.mock(WorkflowInstanceService.class);
    }

    @Bean
    @Primary
    public QualityOrchestrator qualityOrchestrator() {
        return Mockito.mock(QualityOrchestrator.class);
    }

    @Bean
    @Primary
    public CostService costService() {
        return Mockito.mock(CostService.class);
    }
}
