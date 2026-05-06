---
title: RabbitMQ Messaging
type: infrastructure
source: com.schemaplexai.task.config.RabbitMqConfig, CommonConstants
creation_date: 2026-05-06
tags: [mq, rabbitmq, messaging, infrastructure]
confidence: high
---

# RabbitMQ Messaging

RabbitMQ configuration for asynchronous cross-service communication.

## Exchange

| Exchange | Type | Durable | Auto-delete |
|----------|------|---------|-------------|
| `sf.exchange` | Direct | Yes | No |

## Queues and Bindings

| Queue | Routing Key | Purpose | DLX Configured |
|-------|-------------|---------|----------------|
| `sf.agent.execute.queue` | `sf.agent.execute` | Async agent execution dispatch | Yes |
| `sf.workflow.trigger.queue` | `sf.workflow.trigger` | Workflow BPMN trigger events | Yes |
| `sf.notification.queue` | `sf.notification` | Notification delivery | Yes |
| `sf.cost.queue` | `sf.cost` | Cost analytics ingestion | Yes |
| `sf.quality.queue` | `sf.quality` | Quality gate events | Yes |
| `sf.milvus.sync.queue` | `sf.milvus.sync` | Vector store synchronization | Yes |

All queues are:
- **Durable** (`durable=true`) — survive broker restart
- **Non-exclusive** (`exclusive=false`) — shared across consumers
- **Non-auto-delete** (`autoDelete=false`) — persist when last consumer disconnects
- **Dead-letter enabled** — failed messages routed to DLX for retry/analysis

## Routing Keys (Constants)

```java
// CommonConstants.java
RK_AGENT_EXECUTE       = "sf.agent.execute"
RK_AGENT_EXEC_EVENT    = "sf.agent.exec.event"
RK_AGENT_TEAM_CONTEXT  = "sf.agent.team.context"
RK_WORKFLOW_TRIGGER    = "sf.workflow.trigger"
RK_NOTIFICATION        = "sf.notification"
RK_COST                = "sf.cost"
RK_QUALITY             = "sf.quality"
RK_MILVUS_SYNC         = "sf.milvus.sync"
RK_AGENT_CONFIG_SHADOW = "sf.agent.config.shadow"
```

## Publishers

| Publisher | Event Type | Routing Key |
|-----------|-----------|-------------|
| `AgentExecutionEventPublisher` | Execution events | `sf.agent.exec.event` |
| `AgentExecutionEventPublisher` | Shadow config | `sf.agent.config.shadow` |

## Consumers

Consumers are implemented in the `schemaplexai-task` module (MQ consumer service).

## Dead Letter Exchange

All queues declare:
- `x-dead-letter-exchange`: `sf.dlx.exchange`
- `x-dead-letter-routing-key`: `sf.dlx.routing.key`

See `DeadLetterConfig` in `schemaplexai-task` module for DLX queue configuration.

## See Also

- [[services/agent-execution-engine]] — publishes execution events to MQ
- [[services/workflow-node-engine]] — workflow trigger consumer
