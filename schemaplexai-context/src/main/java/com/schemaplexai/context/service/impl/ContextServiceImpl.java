package com.schemaplexai.context.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.schemaplexai.common.context.TenantContextHolder;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.context.entity.SfContext;
import com.schemaplexai.context.mapper.SfContextMapper;
import com.schemaplexai.context.service.ContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Transactional(rollbackFor = Exception.class)
@Service
@RequiredArgsConstructor
public class ContextServiceImpl extends ServiceImpl<SfContextMapper, SfContext> implements ContextService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SfContext ingestContext(Long workspaceId, String name, String type) {
        if (name == null || name.isBlank()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Context name is required");
        }
        SfContext context = new SfContext();
        context.setWorkspaceId(workspaceId);
        context.setName(name.trim());
        context.setType(type);
        String tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            context.setTenantId(tenantId);
        }
        baseMapper.insert(context);
        log.info("Ingested context: id={}, name={}, workspaceId={}", context.getId(), name, workspaceId);
        return context;
    }

    @Override
    public List<SfContext> searchContext(String keyword, String type) {
        LambdaQueryWrapper<SfContext> wrapper = new LambdaQueryWrapper<>();
        String tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            wrapper.eq(SfContext::getTenantId, tenantId);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(SfContext::getName, keyword)
                    .or()
                    .like(SfContext::getType, keyword));
        }
        if (type != null && !type.isBlank()) {
            wrapper.eq(SfContext::getType, type);
        }
        wrapper.orderByDesc(SfContext::getUpdatedAt);
        return baseMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refreshContext(Long contextId) {
        SfContext context = baseMapper.selectById(contextId);
        if (context == null) {
            throw new BaseException(ResultCode.CONTEXT_NOT_FOUND);
        }
        context.setUpdatedAt(LocalDateTime.now());
        baseMapper.updateById(context);
        log.info("Refreshed context: id={}", contextId);
    }

    @Override
    public SfContext getContextByConversation(Long conversationId) {
        // Conversation-to-context mapping: for now, contextId == conversationId
        // This can be refined when a dedicated mapping table is introduced.
        if (conversationId == null) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Conversation ID is required");
        }
        SfContext context = baseMapper.selectById(conversationId);
        if (context == null) {
            throw new BaseException(ResultCode.CONTEXT_NOT_FOUND);
        }
        String tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null && !tenantId.equals(context.getTenantId())) {
            throw new BaseException(ResultCode.FORBIDDEN, "Access denied to context for conversation: " + conversationId);
        }
        return context;
    }
}
