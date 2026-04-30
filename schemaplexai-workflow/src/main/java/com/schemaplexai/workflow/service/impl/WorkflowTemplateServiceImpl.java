package com.schemaplexai.workflow.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.schemaplexai.workflow.entity.SfWorkflowTemplate;
import com.schemaplexai.workflow.mapper.SfWorkflowTemplateMapper;
import com.schemaplexai.workflow.service.WorkflowTemplateService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional(rollbackFor = Exception.class)
@Service
public class WorkflowTemplateServiceImpl extends ServiceImpl<SfWorkflowTemplateMapper, SfWorkflowTemplate> implements WorkflowTemplateService {
}
