package com.schemaplexai.ops.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.schemaplexai.ops.entity.SfArtifact;
import com.schemaplexai.ops.mapper.ArtifactMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional(rollbackFor = Exception.class)
@Service
public class ArtifactServiceImpl extends ServiceImpl<ArtifactMapper, SfArtifact> implements ArtifactService {
}
