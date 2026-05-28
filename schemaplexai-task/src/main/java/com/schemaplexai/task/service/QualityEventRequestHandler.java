package com.schemaplexai.task.service;

import com.schemaplexai.task.mq.dto.QualityEventMessage;

public interface QualityEventRequestHandler {

    void handle(QualityEventMessage message);
}
