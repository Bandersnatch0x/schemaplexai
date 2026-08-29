package com.schemaplexai.agent.config.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Read-only token-usage aggregate over {@code sf_chat_message} (hash-partitioned
 * chat table owned by the agent-engine domain). The aggregate query works across
 * all partitions. Used by the Cockpit statistics endpoint ("Tokens Used").
 *
 * <p>The explicit {@code tenant_id} predicate is deliberate: the MyBatis-Plus
 * tenant interceptor additionally injects the current tenant context condition,
 * so a missing/empty tenant context fails closed (zero rows) rather than
 * leaking cross-tenant totals.</p>
 */
@Mapper
public interface ChatMessageTokenMapper {

    @Select("SELECT COALESCE(SUM(token_count), 0) FROM sf_chat_message WHERE tenant_id = #{tenantId}")
    Long sumTokensByTenant(@Param("tenantId") Long tenantId);
}
