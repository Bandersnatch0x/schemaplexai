package com.schemaplexai.task.mq;

import com.schemaplexai.task.entity.SfMessageFailLog;
import com.schemaplexai.task.mapper.SfMessageFailLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageFailLogService {

    private final SfMessageFailLogMapper messageFailLogMapper;

    public void log(Message message, String consumerGroup, String errorMsg) {
        try {
            SfMessageFailLog record = new SfMessageFailLog();
            record.setMessageId(message.getMessageProperties().getMessageId());
            record.setExchange(message.getMessageProperties().getReceivedExchange());
            record.setRoutingKey(message.getMessageProperties().getReceivedRoutingKey());
            record.setPayload(new String(message.getBody(), StandardCharsets.UTF_8));
            record.setErrorMsg(errorMsg);
            record.setConsumerGroup(consumerGroup);
            record.setStatus("PENDING");
            record.setRetryCount(0);
            messageFailLogMapper.insert(record);
        } catch (Exception ex) {
            log.error("[MessageFailLog] Failed to persist fail log", ex);
        }
    }
}
