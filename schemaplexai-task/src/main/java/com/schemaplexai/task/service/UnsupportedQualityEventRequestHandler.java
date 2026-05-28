package com.schemaplexai.task.service;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.task.mq.dto.QualityEventMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UnsupportedQualityEventRequestHandler implements QualityEventRequestHandler {

    @Override
    public void handle(QualityEventMessage message) {
        log.warn("[QualityEventRequestHandler] Quality event received but no runtime adapter is configured: {}",
                message);
        throw new BaseException(ResultCode.INTERNAL_ERROR,
                "quality event handler is not implemented for eventType=" + message.getEventType());
    }
}
