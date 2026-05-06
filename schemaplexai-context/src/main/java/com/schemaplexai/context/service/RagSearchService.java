package com.schemaplexai.context.service;

import com.schemaplexai.context.entity.KnowledgeChunk;

import java.util.List;

public interface RagSearchService {

    List<KnowledgeChunk> search(String query, String tenantId, int topK);
}
