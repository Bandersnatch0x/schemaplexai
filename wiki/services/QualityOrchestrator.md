---
title: QualityOrchestrator
type: service
source: schemaplexai-quality/src/main/java/com/schemaplexai/quality/service/QualityOrchestrator.java
creation_date: 2026-05-01
tags: [quality, gate, orchestrator, evaluation, core]
confidence: high
---

# QualityOrchestrator

> Evaluates all registered quality gates against an execution context, creating issues for failed rules.

## Responsibilities

1. Load all quality gate definitions from the database
2. Parse each gate's `rulesJson` into a list of rule names
3. Execute each `QualityRule` implementation against the context
4. Create `SfQualityIssue` records for failed checks
5. Return a `QualityReport` summarizing results

## Dependencies

| Dependency | Type | Purpose |
|------------|------|---------|
| `List<QualityRule>` | Collection | All registered quality rule implementations |
| `QualityGateMapper` | Mapper | Load gate definitions |
| `QualityIssueMapper` | Mapper | Persist discovered issues |
| `ObjectMapper` | Jackson | Parse `rulesJson` |

## Methods

### `evaluate(Long executionId, QualityContext context)`

```
1. Load all SfQualityGate records
2. For each gate:
   a. Parse rulesJson → List<String> ruleNames
   b. For each ruleName:
      - Look up QualityRule implementation
      - If missing → log warning, skip
      - Execute rule.check(context)
      - If failed:
        * allPassed = false
        * Insert SfQualityIssue (status = 0 / open)
3. Return QualityReport(executionId, allPassed, results)
```

### `checkQualityGate(Long executionId, String gateName)`

- Creates an empty `QualityContext`
- Calls `evaluate()` and returns `report.isAllPassed()`

### `runQualityPipeline(Long executionId)`

- Creates an empty `QualityContext`
- Calls `evaluate()` for full pipeline execution

## Rule Discovery

- All `QualityRule` beans are injected via constructor
- At `@PostConstruct`, rules are indexed by `ruleName` into a `Map<String, QualityRule>`

## Transactionality

- `@Transactional(rollbackFor = Exception.class)` — issue inserts roll back if any rule throws

## Notes

- The `QualityContext` is currently minimal; future versions will carry execution artifacts, chat history, and metadata.
- Failed rules generate issues with `status = 0` (open) for downstream triage.
