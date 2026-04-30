package com.schemaplexai.quality.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.schemaplexai.quality.entity.SfReviewRecord;
import com.schemaplexai.quality.mapper.ReviewRecordMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional(rollbackFor = Exception.class)
@Service
public class ReviewServiceImpl extends ServiceImpl<ReviewRecordMapper, SfReviewRecord> implements ReviewService {
}
