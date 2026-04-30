package com.schemaplexai.spec.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.spec.dto.DiffHunk;
import com.schemaplexai.spec.dto.SpecDiffResult;
import com.schemaplexai.spec.entity.SfSpec;
import com.schemaplexai.spec.entity.SfSpecVersion;
import com.schemaplexai.spec.mapper.SfSpecMapper;
import com.schemaplexai.spec.mapper.SfSpecVersionMapper;
import com.schemaplexai.spec.service.SpecVersionService;
import com.schemaplexai.spec.util.SpecDiffUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Transactional(rollbackFor = Exception.class)
@Service
@RequiredArgsConstructor
public class SpecVersionServiceImpl extends ServiceImpl<SfSpecVersionMapper, SfSpecVersion> implements SpecVersionService {

    private final SfSpecMapper specMapper;

    @Override
    public SpecDiffResult diff(Long versionAId, Long versionBId) {
        SfSpecVersion vA = baseMapper.selectById(versionAId);
        SfSpecVersion vB = baseMapper.selectById(versionBId);

        if (vA == null || vB == null) {
            throw new BaseException(ResultCode.SPEC_NOT_FOUND, "Spec version not found");
        }

        if (!vA.getSpecId().equals(vB.getSpecId())) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Versions belong to different specs");
        }

        List<DiffHunk> hunks = SpecDiffUtil.diff(vA.getContent(), vB.getContent());
        return new SpecDiffResult(vA.getSpecId(), versionAId, versionBId, hunks);
    }

    @Override
    public SfSpecVersion createVersion(Long specId, String version, String content, String changeLog) {
        SfSpec spec = specMapper.selectById(specId);
        if (spec == null) {
            throw new BaseException(ResultCode.SPEC_NOT_FOUND, "Spec not found: " + specId);
        }

        SfSpecVersion specVersion = new SfSpecVersion();
        specVersion.setSpecId(specId);
        specVersion.setVersion(version);
        specVersion.setContent(content);
        specVersion.setChangeLog(changeLog);
        baseMapper.insert(specVersion);

        // Update spec content and status
        spec.setContent(content);
        spec.setStatus("ACTIVE");
        specMapper.updateById(spec);

        log.info("Created version {} for spec {}", version, specId);
        return specVersion;
    }
}
