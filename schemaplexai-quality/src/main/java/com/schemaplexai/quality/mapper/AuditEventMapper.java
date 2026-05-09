package com.schemaplexai.quality.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.schemaplexai.dao.mapper.BaseMapperX;
import com.schemaplexai.quality.entity.SfAuditEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AuditEventMapper extends BaseMapperX<SfAuditEvent> {

    default List<SfAuditEvent> selectRecentEvents(LocalDateTime since) {
        return selectList(new LambdaQueryWrapper<SfAuditEvent>()
                .ge(SfAuditEvent::getOccurredAt, since)
                .eq(SfAuditEvent::getCorrupted, false)
                .orderByAsc(SfAuditEvent::getOccurredAt));
    }

    @Update("UPDATE sf_audit_event SET corrupted = true WHERE id = #{id}")
    void markCorrupted(Long id);
}
