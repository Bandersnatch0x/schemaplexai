package com.schemaplexai.context.mq;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.context.service.MilvusSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MilvusSyncConsumer {

    private final MilvusSyncService milvusSyncService;

    /**
     * Consume Milvus sync message for queue: sf.milvus.sync
     */
    public void consume(Long docId) {
        if (docId == null) {
            throw new BaseException(ResultCode.PARAM_ERROR, "docId is required");
        }
        log.info("Received Milvus sync message for docId: {}", docId);
        milvusSyncService.syncToMilvus(docId);
    }
}
