package com.schemaplexai.context.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.schemaplexai.context.entity.SfContext;

import java.util.List;

public interface ContextService extends IService<SfContext> {

    /**
     * Ingest a new context into a workspace.
     */
    SfContext ingestContext(Long workspaceId, String name, String type);

    /**
     * Search contexts by name or type within the current tenant.
     */
    List<SfContext> searchContext(String keyword, String type);

    /**
     * Refresh context metadata (e.g., update timestamp, re-index).
     */
    void refreshContext(Long contextId);

    /**
     * Get a context by its associated conversation identifier.
     */
    SfContext getContextByConversation(Long conversationId);
}
