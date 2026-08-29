package com.schemaplexai.agent.engine.config;

import com.schemaplexai.common.context.TenantContextHolder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AgentExecutionAsyncConfig {

    public static final String EXECUTOR_NAME = "agentExecutionExecutor";

    @Bean(name = EXECUTOR_NAME)
    public ThreadPoolTaskExecutor agentExecutionExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("agent-exec-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        // Issue 909 / REQ-20: propagate the caller's tenant context into the async
        // worker thread. TenantContextHolder is a plain ThreadLocal, so without a
        // TaskDecorator the pooled @Async thread sees an empty tenant — breaking
        // tenant-scoped downstream work such as RAG context injection.
        executor.setTaskDecorator(tenantContextPropagatingDecorator());
        executor.initialize();
        return executor;
    }

    /**
     * Captures the submitting thread's tenant at submission time and re-establishes it
     * on the worker thread for the duration of the task, clearing it afterwards so the
     * pooled thread never leaks a tenant into a subsequent task.
     */
    @Bean
    public TaskDecorator tenantContextPropagatingDecorator() {
        return runnable -> {
            String callerTenantId = TenantContextHolder.getTenantId();
            return () -> {
                if (callerTenantId != null) {
                    TenantContextHolder.setTenantId(callerTenantId);
                }
                try {
                    runnable.run();
                } finally {
                    TenantContextHolder.clear();
                }
            };
        };
    }
}
