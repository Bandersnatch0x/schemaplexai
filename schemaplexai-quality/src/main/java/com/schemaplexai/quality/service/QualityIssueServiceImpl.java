package com.schemaplexai.quality.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.schemaplexai.quality.entity.SfQualityIssue;
import com.schemaplexai.quality.mapper.QualityIssueMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional(rollbackFor = Exception.class)
@Service
public class QualityIssueServiceImpl extends ServiceImpl<QualityIssueMapper, SfQualityIssue> implements QualityIssueService {
}
