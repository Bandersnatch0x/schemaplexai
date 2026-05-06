package com.schemaplexai.admin.mapper;

import com.schemaplexai.admin.entity.SfAuditLog;
import com.schemaplexai.dao.mapper.BaseMapperX;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface SfAuditLogMapper extends BaseMapperX<SfAuditLog> {

    @Select("SELECT COUNT(*) FROM sf_audit_log WHERE action = #{action} AND executed_at >= #{since} AND deleted = 0")
    Long countByActionSince(@Param("action") String action, @Param("since") LocalDateTime since);

    @Select("SELECT action, COUNT(*) as count FROM sf_audit_log WHERE executed_at >= #{since} AND deleted = 0 GROUP BY action")
    List<ActionCount> countByActionsSince(@Param("since") LocalDateTime since);

    @Select("SELECT * FROM sf_audit_log WHERE tenant_id = #{tenantId} AND deleted = 0 ORDER BY executed_at DESC LIMIT #{limit}")
    List<SfAuditLog> selectRecentByTenantId(@Param("tenantId") String tenantId, @Param("limit") Integer limit);

    record ActionCount(String action, Long count) {}
}
