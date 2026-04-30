package com.schemaplexai.context.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.schemaplexai.context.entity.SfContext;
import com.schemaplexai.context.mapper.SfContextMapper;
import com.schemaplexai.context.service.ContextService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional(rollbackFor = Exception.class)
@Service
public class ContextServiceImpl extends ServiceImpl<SfContextMapper, SfContext> implements ContextService {
}
