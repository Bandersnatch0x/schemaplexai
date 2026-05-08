package com.schemaplexai.context.service;

public interface MilvusSyncService {

    void syncToMilvus(Long docId);

    void deleteByDocId(Long docId);

    void reSyncDoc(Long docId);
}
