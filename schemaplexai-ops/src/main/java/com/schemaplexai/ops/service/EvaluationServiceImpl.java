package com.schemaplexai.ops.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.schemaplexai.ops.entity.SfEvalTask;
import com.schemaplexai.ops.mapper.EvalTaskMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional(rollbackFor = Exception.class)
@Service
public class EvaluationServiceImpl extends ServiceImpl<EvalTaskMapper, SfEvalTask> implements EvaluationService {
}
