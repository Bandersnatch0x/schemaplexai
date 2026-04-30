package com.schemaplexai.quality.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.schemaplexai.quality.entity.SfSecurityPolicy;
import com.schemaplexai.quality.mapper.SecurityPolicyMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional(rollbackFor = Exception.class)
@Service
public class SecurityPolicyServiceImpl extends ServiceImpl<SecurityPolicyMapper, SfSecurityPolicy> implements SecurityPolicyService {
}
