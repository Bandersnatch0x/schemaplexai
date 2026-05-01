package com.schemaplexai.agent.engine.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {AgentExecutionAsyncConfig.class})
class AgentExecutionAsyncConfigTest {

    @Autowired
    private ThreadPoolTaskExecutor agentExecutionExecutor;

    @Test
    void beanShouldExist() {
        assertNotNull(agentExecutionExecutor, "agentExecutionExecutor bean should exist");
    }

    @Test
    void corePoolSizeShouldBe10() {
        assertEquals(10, agentExecutionExecutor.getCorePoolSize(),
                "Core pool size should be 10");
    }

    @Test
    void maxPoolSizeShouldBe50() {
        assertEquals(50, agentExecutionExecutor.getMaxPoolSize(),
                "Max pool size should be 50");
    }

    @Test
    void queueCapacityShouldBe200() {
        assertEquals(200, agentExecutionExecutor.getThreadPoolExecutor().getQueue().remainingCapacity() + agentExecutionExecutor.getThreadPoolExecutor().getQueue().size(),
                "Queue capacity should be 200");
    }

    @Test
    void threadNamePrefixShouldBeAgentExec() {
        assertTrue(agentExecutionExecutor.getThreadNamePrefix().startsWith("agent-exec-"),
                "Thread name prefix should start with 'agent-exec-'");
    }

    @Test
    void rejectionPolicyShouldBeCallerRuns() {
        ThreadPoolExecutor executor = agentExecutionExecutor.getThreadPoolExecutor();
        assertTrue(executor.getRejectedExecutionHandler() instanceof ThreadPoolExecutor.CallerRunsPolicy,
                "Rejection policy should be CallerRunsPolicy");
    }
}
