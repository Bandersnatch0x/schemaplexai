---
title: SecurityPolicyController
type: controller
source: schemaplexai-quality/src/main/java/com/schemaplexai/quality/controller/SecurityPolicyController.java
creation_date: 2026-05-01
tags: [quality, security, policy, controller, crud]
confidence: high
---

# SecurityPolicyController

> CRUD controller for security policy definitions.

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/quality/security-policies` | Create a new security policy |
| PUT | `/quality/security-policies/{id}` | Update an existing policy by ID |
| DELETE | `/quality/security-policies/{id}` | Delete a policy by ID |
| GET | `/quality/security-policies/{id}` | Get a single policy by ID |
| GET | `/quality/security-policies` | List all security policies |

## DTO / Entity

- **Request/Response**: `SfSecurityPolicy` entity
  - `name` (String): Policy name
  - `policyType` (String): Type/category of security policy
  - `rulesJson` (String): JSON-serialized policy rules
  - `status` (Integer): Policy status
  - Inherits `BaseEntity`

## Service Dependencies

- `SecurityPolicyService` — MyBatis-Plus `IService` for `SfSecurityPolicy`

## Notes

- Security policies are evaluated as part of the quality gate pipeline.
- `rulesJson` stores structured rule definitions for security scanning.
