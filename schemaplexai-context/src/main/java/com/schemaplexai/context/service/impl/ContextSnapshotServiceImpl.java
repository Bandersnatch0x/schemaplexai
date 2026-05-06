package com.schemaplexai.context.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.schemaplexai.common.context.TenantContextHolder;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.context.entity.SfContext;
import com.schemaplexai.context.entity.SfContextSnapshot;
import com.schemaplexai.context.mapper.SfContextMapper;
import com.schemaplexai.context.mapper.SfContextSnapshotMapper;
import com.schemaplexai.context.service.ContextSnapshotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Transactional(rollbackFor = Exception.class)
@Service
@RequiredArgsConstructor
public class ContextSnapshotServiceImpl extends ServiceImpl<SfContextSnapshotMapper, SfContextSnapshot> implements ContextSnapshotService {

    private final SfContextMapper contextMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SfContextSnapshot createSnapshot(Long contextId, String snapshotJson) {
        if (contextId == null) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Context ID is required");
        }
        SfContext context = contextMapper.selectById(contextId);
        if (context == null) {
            throw new BaseException(ResultCode.CONTEXT_NOT_FOUND);
        }
        String tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null && !tenantId.equals(context.getTenantId())) {
            throw new BaseException(ResultCode.FORBIDDEN, "Access denied to context: " + contextId);
        }

        Integer maxVersion = baseMapper.selectList(
                        new LambdaQueryWrapper<SfContextSnapshot>()
                                .eq(SfContextSnapshot::getContextId, contextId)
                                .orderByDesc(SfContextSnapshot::getVersion)
                                .last("LIMIT 1"))
                .stream()
                .findFirst()
                .map(SfContextSnapshot::getVersion)
                .orElse(0);

        SfContextSnapshot snapshot = new SfContextSnapshot();
        snapshot.setContextId(contextId);
        snapshot.setSnapshotJson(snapshotJson);
        snapshot.setVersion(maxVersion + 1);
        if (tenantId != null) {
            snapshot.setTenantId(tenantId);
        }
        baseMapper.insert(snapshot);
        log.info("Created snapshot: id={}, contextId={}, version={}", snapshot.getId(), contextId, snapshot.getVersion());
        return snapshot;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String restoreFromSnapshot(Long snapshotId) {
        SfContextSnapshot snapshot = baseMapper.selectById(snapshotId);
        if (snapshot == null) {
            throw new BaseException(ResultCode.NOT_FOUND, "Snapshot not found: " + snapshotId);
        }
        String tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null && !tenantId.equals(snapshot.getTenantId())) {
            throw new BaseException(ResultCode.FORBIDDEN, "Access denied to snapshot: " + snapshotId);
        }
        log.info("Restored from snapshot: id={}, contextId={}, version={}", snapshotId, snapshot.getContextId(), snapshot.getVersion());
        return snapshot.getSnapshotJson();
    }

    @Override
    public List<SfContextSnapshot> listSnapshotsByContext(Long contextId) {
        if (contextId == null) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Context ID is required");
        }
        return baseMapper.selectList(
                new LambdaQueryWrapper<SfContextSnapshot>()
                        .eq(SfContextSnapshot::getContextId, contextId)
                        .orderByDesc(SfContextSnapshot::getVersion));
    }

    @Override
    public String compareSnapshots(Long snapshotIdA, Long snapshotIdB) {
        SfContextSnapshot snapshotA = baseMapper.selectById(snapshotIdA);
        SfContextSnapshot snapshotB = baseMapper.selectById(snapshotIdB);
        if (snapshotA == null || snapshotB == null) {
            throw new BaseException(ResultCode.NOT_FOUND, "One or both snapshots not found");
        }
        if (!snapshotA.getContextId().equals(snapshotB.getContextId())) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Snapshots must belong to the same context");
        }
        String tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            if (!tenantId.equals(snapshotA.getTenantId()) || !tenantId.equals(snapshotB.getTenantId())) {
                throw new BaseException(ResultCode.FORBIDDEN, "Access denied to one or both snapshots");
            }
        }

        String jsonA = snapshotA.getSnapshotJson() == null ? "" : snapshotA.getSnapshotJson();
        String jsonB = snapshotB.getSnapshotJson() == null ? "" : snapshotB.getSnapshotJson();
        boolean equal = jsonA.equals(jsonB);

        StringBuilder diff = new StringBuilder();
        diff.append("Comparison between snapshot v").append(snapshotA.getVersion())
                .append(" and v").append(snapshotB.getVersion()).append("\n");
        diff.append("Equal: ").append(equal).append("\n");
        if (!equal) {
            diff.append("Length A: ").append(jsonA.length()).append(", Length B: ").append(jsonB.length()).append("\n");
        }
        return diff.toString();
    }
}
