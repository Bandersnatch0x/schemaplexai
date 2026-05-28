package com.schemaplexai.task.service;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.task.mq.dto.MilvusSyncMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UnsupportedMilvusSyncRequestHandler implements MilvusSyncRequestHandler {

    @Override
    public void handle(MilvusSyncMessage message) {
        log.warn("[MilvusSyncRequestHandler] Milvus sync request received but no runtime adapter is configured: {}",
                message);
        throw new BaseException(ResultCode.INTERNAL_ERROR,
                "milvus sync handler is not implemented for operation=" + message.getOperation());
    }
}
