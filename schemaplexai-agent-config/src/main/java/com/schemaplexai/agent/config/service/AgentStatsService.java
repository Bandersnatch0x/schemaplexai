package com.schemaplexai.agent.config.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.schemaplexai.agent.config.dto.AgentStatsVO;
import com.schemaplexai.agent.config.entity.SfAgent;
import com.schemaplexai.agent.config.entity.SfAgentExecution;
import com.schemaplexai.agent.config.mapper.ChatMessageTokenMapper;
import com.schemaplexai.agent.config.mapper.AgentExecutionStatsMapper;
import com.schemaplexai.agent.config.mapper.SfAgentMapper;
import com.schemaplexai.common.context.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Tenant-scoped agent statistics for the Cockpit page (issue 927).
 *
 * <p>All queries are tenant-isolated: the MyBatis-Plus
 * {@code TenantLineInnerInterceptor} injects the {@code tenant_id} predicate
 * from {@link TenantContextHolder} into every statement, and the token
 * aggregate additionally passes the tenant explicitly. An empty tenant
 * context therefore yields zeros rather than cross-tenant data.</p>
 *
 * <p>State/reason literals mirror the agent-engine enums
 * ({@code AgentExecutionState.isTerminal()}, {@code PauseReason}) but are kept
 * as local constants so this read-only stats path does not compile-couple to
 * engine internals.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentStatsService {

    /** Mirrors agent-engine {@code AgentExecutionState.isTerminal()}. */
    static final List<String> TERMINAL_STATES = List.of("COMPLETED", "FAILED", "CANCELLED", "REJECTED");
    static final String STATE_PAUSED = "PAUSED";
    static final String PAUSE_REASON_MANUAL_APPROVAL = "MANUAL_APPROVAL_REQUIRED";
    static final String AGENT_STATUS_ACTIVE = "ACTIVE";

    private final SfAgentMapper agentMapper;
    private final AgentExecutionStatsMapper executionMapper;
    private final ChatMessageTokenMapper tokenMapper;

    public AgentStatsVO getStats() {
        long totalAgents = zeroIfNull(agentMapper.selectCount(null));
        long activeAgents = zeroIfNull(agentMapper.selectCount(
                new LambdaQueryWrapper<SfAgent>().eq(SfAgent::getStatus, AGENT_STATUS_ACTIVE)));

        long totalExecutions = zeroIfNull(executionMapper.selectCount(null));
        long runningExecutions = zeroIfNull(executionMapper.selectCount(
                new LambdaQueryWrapper<SfAgentExecution>().notIn(SfAgentExecution::getState, TERMINAL_STATES)));
        long todayExecutions = zeroIfNull(executionMapper.selectCount(
                new LambdaQueryWrapper<SfAgentExecution>().apply("created_at >= CURRENT_DATE")));
        long pendingApprovals = zeroIfNull(executionMapper.selectCount(
                new LambdaQueryWrapper<SfAgentExecution>()
                        .eq(SfAgentExecution::getState, STATE_PAUSED)
                        .eq(SfAgentExecution::getPauseReason, PAUSE_REASON_MANUAL_APPROVAL)));

        return AgentStatsVO.builder()
                .totalAgents(totalAgents)
                .activeAgents(activeAgents)
                .runningExecutions(runningExecutions)
                .totalExecutions(totalExecutions)
                .todayExecutions(todayExecutions)
                .totalTokens(sumTokensForCurrentTenant())
                .pendingApprovals(pendingApprovals)
                .build();
    }

    private long sumTokensForCurrentTenant() {
        Long tenantId = parseTenantId(TenantContextHolder.getTenantId());
        if (tenantId == null) {
            return 0L;
        }
        return zeroIfNull(tokenMapper.sumTokensByTenant(tenantId));
    }

    private static Long parseTenantId(String tenantId) {
        if (!StringUtils.hasText(tenantId)) {
            return null;
        }
        try {
            return Long.valueOf(tenantId.trim());
        } catch (NumberFormatException e) {
            log.warn("Non-numeric tenant id in context, skipping token aggregate: {}", tenantId);
            return null;
        }
    }

    private static long zeroIfNull(Long count) {
        return count == null ? 0L : count;
    }
}
