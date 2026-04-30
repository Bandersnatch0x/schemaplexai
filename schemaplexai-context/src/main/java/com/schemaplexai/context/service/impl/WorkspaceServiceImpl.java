package com.schemaplexai.context.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.schemaplexai.context.entity.SfWorkspace;
import com.schemaplexai.context.mapper.SfWorkspaceMapper;
import com.schemaplexai.context.service.WorkspaceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional(rollbackFor = Exception.class)
@Service
public class WorkspaceServiceImpl extends ServiceImpl<SfWorkspaceMapper, SfWorkspace> implements WorkspaceService {
}
