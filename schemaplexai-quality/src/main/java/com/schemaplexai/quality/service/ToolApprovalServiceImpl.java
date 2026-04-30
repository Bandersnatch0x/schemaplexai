package com.schemaplexai.quality.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.schemaplexai.quality.entity.SfToolApprovalAmendment;
import com.schemaplexai.quality.mapper.ToolApprovalAmendmentMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional(rollbackFor = Exception.class)
@Service
public class ToolApprovalServiceImpl extends ServiceImpl<ToolApprovalAmendmentMapper, SfToolApprovalAmendment> implements ToolApprovalService {
}
