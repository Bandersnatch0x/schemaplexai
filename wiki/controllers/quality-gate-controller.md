---
title: QualityGateController
type: controller
source: schemaplexai-quality/src/main/java/com/schemaplexai/quality/controller/QualityGateController.java
creation_date: 2026-05-07
update_date: 2026-05-07
tags: [controller, quality, gate, evaluation, rules]
confidence: high
---

# QualityGateController

> One-sentence summary: Quality gate evaluation controller managing rule-based quality thresholds for AI-generated outputs and pipeline stage gating.

## Base Path

`/quality/gates` (routed via Gateway to `schemaplexai-quality` port 8090)

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/` | Create a new quality gate |
| PUT | `/{id}` | Update an existing quality gate |
| DELETE | `/{id}` | Delete a quality gate |
| GET | `/{id}` | Get quality gate by ID |
| GET | `/` | List all quality gates |

## Key Request/Response DTOs

### SfQualityGate (Request/Response Entity)
```java
@TableName("sf_quality_gate")
public class SfQualityGate extends BaseEntity {
    private String name;       // Gate name / label
    private String rulesJson;  // Quality rules definition (JSON)
    private Integer status;    // Gate status (active/inactive)
}
```

### Responses
- `Result<Long>` — created gate ID
- `Result<Boolean>` — update/delete success
- `Result<SfQualityGate>` — single gate retrieval
- `Result<List<SfQualityGate>>` — list all gates

## Dependencies

- `QualityGateService qualityGateService` — handles quality gate persistence and rule evaluation

## Notes

- Returns `Result<T>` wrapper (see [[architecture]])
- `rulesJson` stores structured quality criteria (e.g., coverage thresholds, security checks, style rules)
- Gates can be applied to spec reviews, code generation, or workflow stages
- 404 returned as `ResultCode.NOT_FOUND` when gate not found

## Backlinks

- Quality issues: [[controllers/QualityIssueController]]
- Security policies: [[controllers/SecurityPolicyController]]
- Review management: [[controllers/ReviewController]]
- See [[routes]] for routing
