package com.schemaplexai.quality.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.model.event.ApprovalDecisionEvent;
import com.schemaplexai.quality.entity.ApprovalTicket;
import com.schemaplexai.quality.mapper.ApprovalTicketMapper;
import org.springframework.stereotype.Service;

/**
 * Validates approval decision protocol (version, ordering, etc.).
 * Phase 1: basic field validation and ticket lookup.
 */
@Service
public class ApprovalDecisionValidator {

    private final ApprovalTicketMapper approvalTicketMapper;

    public ApprovalDecisionValidator(ApprovalTicketMapper approvalTicketMapper) {
        this.approvalTicketMapper = approvalTicketMapper;
    }

    public void validate(ApprovalDecisionEvent event) {
        if (event == null) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Approval decision event must not be null");
        }

        if (event.ticketId() == null) {
            throw new BaseException(ResultCode.PARAM_ERROR, "ticketId must not be null");
        }

        if (event.decisionVersion() <= 0) {
            return;
        }

        if (event.expectedExecutionVersion() <= 0) {
            return;
        }

        if (approvalTicketMapper == null) {
            return;
        }

        ApprovalTicket ticket = approvalTicketMapper.selectOne(
                new QueryWrapper<ApprovalTicket>().eq("ticket_id", event.ticketId())
        );

        if (ticket == null) {
            throw new BaseException(ResultCode.NOT_FOUND, "Approval ticket not found: " + event.ticketId());
        }
    }
}
