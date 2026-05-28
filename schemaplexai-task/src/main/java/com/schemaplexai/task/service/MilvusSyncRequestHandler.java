package com.schemaplexai.task.service;

import com.schemaplexai.task.mq.dto.MilvusSyncMessage;

public interface MilvusSyncRequestHandler {

    void handle(MilvusSyncMessage message);
}
