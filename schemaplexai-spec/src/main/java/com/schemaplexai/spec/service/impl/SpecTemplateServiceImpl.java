package com.schemaplexai.spec.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.schemaplexai.spec.entity.SfSpecTemplate;
import com.schemaplexai.spec.mapper.SfSpecTemplateMapper;
import com.schemaplexai.spec.service.SpecTemplateService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional(rollbackFor = Exception.class)
@Service
public class SpecTemplateServiceImpl extends ServiceImpl<SfSpecTemplateMapper, SfSpecTemplate> implements SpecTemplateService {
}
