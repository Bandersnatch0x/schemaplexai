package com.schemaplexai.context.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.schemaplexai.context.entity.SfContextSnapshot;
import com.schemaplexai.context.mapper.SfContextSnapshotMapper;
import com.schemaplexai.context.service.ContextSnapshotService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional(rollbackFor = Exception.class)
@Service
public class ContextSnapshotServiceImpl extends ServiceImpl<SfContextSnapshotMapper, SfContextSnapshot> implements ContextSnapshotService {
}
