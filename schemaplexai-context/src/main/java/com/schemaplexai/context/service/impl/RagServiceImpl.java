package com.schemaplexai.context.service.impl;

import com.schemaplexai.context.entity.SfKnowledgeDoc;
import com.schemaplexai.context.mapper.SfKnowledgeDocMapper;
import com.schemaplexai.context.service.RagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Transactional(rollbackFor = Exception.class)
@Service
@RequiredArgsConstructor
public class RagServiceImpl implements RagService {

    private final SfKnowledgeDocMapper knowledgeDocMapper;

    @Override
    public List<String> retrieve(String query, int topK) {
        log.info("RAG retrieve query: {}, topK: {}", query, topK);

        if (query == null || query.isBlank()) {
            return List.of();
        }

        // Phase 1: Simple keyword-based retrieval
        // Phase 2 (TODO): Vector-based retrieval via Milvus once embedding service is ready

        List<SfKnowledgeDoc> docs = knowledgeDocMapper.selectList(null);
        String lowerQuery = query.toLowerCase();

        return docs.stream()
                .filter(doc -> matchesQuery(doc, lowerQuery))
                .limit(topK)
                .map(doc -> doc.getTitle() + " | " + doc.getFileName())
                .collect(Collectors.toList());
    }

    private boolean matchesQuery(SfKnowledgeDoc doc, String lowerQuery) {
        if (doc.getTitle() != null && doc.getTitle().toLowerCase().contains(lowerQuery)) {
            return true;
        }
        if (doc.getFileName() != null && doc.getFileName().toLowerCase().contains(lowerQuery)) {
            return true;
        }
        if (doc.getDocType() != null && doc.getDocType().toLowerCase().contains(lowerQuery)) {
            return true;
        }
        return false;
    }
}
