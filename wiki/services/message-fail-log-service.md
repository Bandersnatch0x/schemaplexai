---
title: MessageFailLogService
type: service
source: schemaplexai-task/src/main/java/com/schemaplexai/task/mq/MessageFailLogService.java
creation_date: 2026-05-07
update_date: 2026-05-07
tags: [service, task, mq, rabbitmq, retry, dead-letter]
confidence: high
---

# MessageFailLogService

> One-sentence summary: Persists failed RabbitMQ message details for dead-letter queue analysis and retry tracking, with defensive error handling to prevent logging failures from cascading.

## Responsibilities

1. Log failed RabbitMQ messages with full metadata (exchange, routing key, payload)
2. Track consumer group and retry count for failure analysis
3. Prevent logging failures from cascading by catching and logging exceptions

## Key Methods

| Method | Description | Parameters | Return |
|--------|-------------|------------|--------|
| `log` | Persist a failed message to the fail log table | `message` — the RabbitMQ `Message` object; `consumerGroup` — consumer group name; `errorMsg` — error description | `void` |

## Key Classes

| Class | Path | Role |
|-------|------|------|
| `MessageFailLogService` | `schemaplexai-task/src/main/java/com/schemaplexai/task/mq/MessageFailLogService.java` | Service class for message failure logging |
| `SfMessageFailLog` | `schemaplexai-task/src/main/java/com/schemaplexai/task/entity/SfMessageFailLog.java` | Entity: `messageId`, `exchange`, `routingKey`, `payload`, `errorMsg`, `consumerGroup`, `status`, `retryCount` |
| `SfMessageFailLogMapper` | `schemaplexai-task/src/main/java/com/schemaplexai/task/mapper/SfMessageFailLogMapper.java` | MyBatis-Plus mapper |

## Logged Fields

| Field | Source | Description |
|-------|--------|-------------|
| `messageId` | `message.getMessageProperties().getMessageId()` | Unique message identifier |
| `exchange` | `message.getMessageProperties().getReceivedExchange()` | Exchange the message was received from |
| `routingKey` | `message.getMessageProperties().getReceivedRoutingKey()` | Routing key used |
| `payload` | `message.getBody()` (UTF-8 decoded) | Message body content |
| `errorMsg` | Parameter | Error description from consumer |
| `consumerGroup` | Parameter | Consumer group that failed |
| `status` | Hardcoded `"PENDING"` | Initial retry status |
| `retryCount` | Hardcoded `0` | Initial retry count |

## Error Handling

- All exceptions during logging are caught and logged via SLF4J to prevent cascading failures
- Failed log persistence does not propagate the exception to the caller

## Dependencies / Collaborators

- **Spring AMQP** — `Message` and `MessageProperties` for RabbitMQ message metadata
- **MyBatis-Plus** — `SfMessageFailLogMapper` for persistence
- **Slf4j** — logging for failure cases within the logger itself

## Backlinks

- [[services/task-service]] — task scheduling and MQ consumers
- [[entities/message-fail-log]] — message failure log entity
