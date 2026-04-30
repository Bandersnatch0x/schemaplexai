package com.schemaplexai.spec.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.schemaplexai.spec.entity.SfSpec;
import com.schemaplexai.spec.mapper.SfSpecMapper;
import com.schemaplexai.spec.service.SpecService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional(rollbackFor = Exception.class)
@Service
public class SpecServiceImpl extends ServiceImpl<SfSpecMapper, SfSpec> implements SpecService {
}
