---
title: DeliveryService
type: service
source: schemaplexai-ops/src/main/java/com/schemaplexai/ops/service/DeliveryService.java
creation_date: 2026-05-07
update_date: 2026-05-07
tags: [service, ops, delivery, artifact, tracking]
confidence: high
---

# DeliveryService

> One-sentence summary: Manages artifact delivery records with status tracking from creation through confirmation.

## Responsibilities

1. Create delivery records linking artifacts to recipients
2. Track delivery status updates throughout the lifecycle
3. Confirm deliveries as completed
4. List delivery records filtered by status

## Key Methods

| Method | Description | Parameters | Return |
|--------|-------------|------------|--------|
| `createDelivery` | Create a delivery record for an artifact | `artifactId` (Long), `deliveryType` (String), `recipient` (String) | `SfDeliveryRecord` |
| `trackDelivery` | Update the status of a delivery record | `deliveryId` (Long), `status` (Integer) | `SfDeliveryRecord` |
| `confirmDelivery` | Mark a delivery as completed | `deliveryId` (Long) | `SfDeliveryRecord` |
| `listByStatus` | List delivery records by status code | `status` (Integer) | `List<SfDeliveryRecord>` |

## Dependencies / Collaborators

- **Entity**: `SfDeliveryRecord` — delivery record persistence via MyBatis-Plus `IService`

## Related

- [[services/artifact-service]] — source of artifacts being delivered
- [[services/notification-service]] — may notify recipients of delivery status
- [[entities/ops]] — ops domain entities
