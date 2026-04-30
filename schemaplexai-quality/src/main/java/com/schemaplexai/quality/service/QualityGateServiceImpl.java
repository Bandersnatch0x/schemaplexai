package com.schemaplexai.quality.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.schemaplexai.quality.entity.SfQualityGate;
import com.schemaplexai.quality.mapper.QualityGateMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional(rollbackFor = Exception.class)
@Service
public class QualityGateServiceImpl extends ServiceImpl<QualityGateMapper, SfQualityGate> implements QualityGateService {
}
