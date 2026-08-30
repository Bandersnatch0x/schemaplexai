package com.schemaplexai.agent.config.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.schemaplexai.agent.config.dto.AgentStatsVO;
import com.schemaplexai.agent.config.entity.SfAgent;
import com.schemaplexai.agent.config.entity.SfAgentExecution;
import com.schemaplexai.agent.config.mapper.ChatMessageTokenMapper;
import com.schemaplexai.agent.config.mapper.AgentExecutionStatsMapper;
import com.schemaplexai.agent.config.mapper.SfAgentMapper;
import com.schemaplexai.common.context.TenantContextHolder;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgentStatsServiceTest {

    @Mock
    private SfAgentMapper agentMapper;

    @Mock
    private AgentExecutionStatsMapper executionMapper;

    @Mock
    private ChatMessageTokenMapper tokenMapper;

    @InjectMocks
    private AgentStatsService agentStatsService;

    @BeforeAll
    static void initTableInfo() {
        // Enables LambdaQueryWrapper column resolution in pure unit tests
        MybatisConfiguration configuration = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(configuration, ""), SfAgent.class);
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(configuration, ""), SfAgentExecution.class);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    // ========== full stats aggregation ==========

    @Test
    void shouldAggregateAllStatsFromRealQueries() {
        TenantContextHolder.setTenantId("42");
        // first call = totalAgents, second call = totalExecutions
        when(agentMapper.selectCount(isNull())).thenReturn(7L, 7L);
        when(agentMapper.selectCount(any(LambdaQueryWrapper.class))).thenAnswer(inv -> {
            String sql = ((LambdaQueryWrapper<?>) inv.getArgument(0)).getSqlSegment();
            assertThat(sql).contains("status"); // ACTIVE agent filter
            return 5L;
        });
        when(executionMapper.selectCount(isNull())).thenReturn(120L);
        when(executionMapper.selectCount(any(LambdaQueryWrapper.class))).thenAnswer(inv -> {
            String sql = ((LambdaQueryWrapper<?>) inv.getArgument(0)).getSqlSegment();
            if (sql.contains("NOT IN")) {
                return 3L; // running = non-terminal states
            }
            if (sql.contains("CURRENT_DATE")) {
                return 9L; // today's executions
            }
            assertThat(sql).contains("state").contains("pause_reason");
            return 2L; // paused awaiting manual approval
        });
        when(tokenMapper.sumTokensByTenant(42L)).thenReturn(12345L);

        AgentStatsVO stats = agentStatsService.getStats();

        assertThat(stats.getTotalAgents()).isEqualTo(7L);
        assertThat(stats.getActiveAgents()).isEqualTo(5L);
        assertThat(stats.getRunningExecutions()).isEqualTo(3L);
        assertThat(stats.getTotalExecutions()).isEqualTo(120L);
        assertThat(stats.getTodayExecutions()).isEqualTo(9L);
        assertThat(stats.getPendingApprovals()).isEqualTo(2L);
        assertThat(stats.getTotalTokens()).isEqualTo(12345L);
        verify(tokenMapper).sumTokensByTenant(42L);
    }

    @Test
    void shouldExcludeTerminalStatesFromRunningCount() {
        when(agentMapper.selectCount(isNull())).thenReturn(0L);
        when(agentMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(executionMapper.selectCount(isNull())).thenReturn(0L);
        when(executionMapper.selectCount(any(LambdaQueryWrapper.class))).thenAnswer(inv -> {
            String sql = ((LambdaQueryWrapper<?>) inv.getArgument(0)).getSqlSegment();
            if (sql.contains("NOT IN")) {
                for (String terminal : AgentStatsService.TERMINAL_STATES) {
                    assertThat(sql).doesNotContain(terminal);
                }
                return 1L;
            }
            return 0L;
        });

        AgentStatsVO stats = agentStatsService.getStats();

        assertThat(stats.getRunningExecutions()).isEqualTo(1L);
    }

    // ========== tenant context handling for token aggregate ==========

    @Test
    void shouldSkipTokenQueryWhenTenantContextMissing() {
        when(agentMapper.selectCount(isNull())).thenReturn(1L);
        when(agentMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        when(executionMapper.selectCount(isNull())).thenReturn(2L);
        when(executionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        AgentStatsVO stats = agentStatsService.getStats();

        assertThat(stats.getTotalTokens()).isZero();
        verify(tokenMapper, never()).sumTokensByTenant(any());
    }

    @Test
    void shouldReturnZeroTokensForNonNumericTenant() {
        TenantContextHolder.setTenantId("tenant-x");
        when(agentMapper.selectCount(isNull())).thenReturn(1L);
        when(agentMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        when(executionMapper.selectCount(isNull())).thenReturn(2L);
        when(executionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        AgentStatsVO stats = agentStatsService.getStats();

        assertThat(stats.getTotalTokens()).isZero();
        verify(tokenMapper, never()).sumTokensByTenant(any());
    }

    // ========== null-safety ==========

    @Test
    void shouldTreatNullCountsAsZero() {
        when(agentMapper.selectCount(isNull())).thenReturn(null);
        when(agentMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(executionMapper.selectCount(isNull())).thenReturn(null);
        when(executionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(null);

        AgentStatsVO stats = agentStatsService.getStats();

        assertThat(stats.getTotalAgents()).isZero();
        assertThat(stats.getActiveAgents()).isZero();
        assertThat(stats.getRunningExecutions()).isZero();
        assertThat(stats.getTotalExecutions()).isZero();
        assertThat(stats.getTodayExecutions()).isZero();
        assertThat(stats.getPendingApprovals()).isZero();
        assertThat(stats.getTotalTokens()).isZero();
    }

    @Test
    void shouldTreatNullTokenSumAsZero() {
        TenantContextHolder.setTenantId("42");
        when(agentMapper.selectCount(isNull())).thenReturn(0L);
        when(agentMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(executionMapper.selectCount(isNull())).thenReturn(0L);
        when(executionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(tokenMapper.sumTokensByTenant(42L)).thenReturn(null);

        AgentStatsVO stats = agentStatsService.getStats();

        assertThat(stats.getTotalTokens()).isZero();
    }
}
