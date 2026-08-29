package com.schemaplexai.web.mapper;

import com.schemaplexai.quality.entity.ApprovalTicket;
import com.schemaplexai.web.vo.ApprovalVO;
import org.springframework.stereotype.Component;

/**
 * M6.4: Approval ticket entity to VO mapper.
 */
@Component
public class ApprovalMapper {

    public ApprovalVO toApprovalVO(ApprovalTicket entity) {
        if (entity == null) {
            return null;
        }
        ApprovalVO vo = new ApprovalVO();
        vo.setTicketId(entity.getTicketId() != null ? entity.getTicketId().toString() : null);
        vo.setExecutionId(entity.getExecutionId());
        vo.setAgentId(entity.getAgentId());
        vo.setStage(entity.getStage());
        vo.setHandler(entity.getHandler());
        vo.setRiskLevel(entity.getRiskLevel());
        vo.setActionDescription(entity.getActionDescription());
        vo.setTriggeringSeq(entity.getTriggeringSeq());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
