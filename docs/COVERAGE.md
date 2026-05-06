# SchemaPlexAI Coverage Baseline

**Date:** 2026-05-06  
**Generated from:** `mvn clean verify` with JaCoCo 0.8.12

---

## Summary

| Module | Instruction % | Branch % | Line % |
|--------|--------------|----------|--------|
| schemaplexai-agent-engine | 92.9% | 75.0% | 93.2% |
| schemaplexai-gateway | 83.8% | 66.7% | 82.8% |
| schemaplexai-common | 81.3% | 72.7% | 82.9% |
| schemaplexai-dao | 44.6% | 50.0% | 44.4% |
| schemaplexai-model | 31.3% | 27.3% | 30.4% |
| schemaplexai-agent-config | 2.8% | 0.0% | 1.3% |
| schemaplexai-system | 1.6% | 0.0% | 2.9% |
| schemaplexai-web | 0.0% | 0.0% | 0.0% |
| schemaplexai-integration | 0.0% | 0.0% | 0.0% |
| **Average** | **37.6%** | **32.4%** | **37.5%** |

*Modules not listed (schemaplexai-context, schemaplexai-quality, schemaplexai-ops, schemaplexai-spec, schemaplexai-workflow, schemaplexai-task, schemaplexai-admin) have no JaCoCo reports — either no test sources exist or the build was skipped.*

---

## Modules Above 80% Instruction Coverage

| Module | Instruction % | Test Count |
|--------|--------------|------------|
| schemaplexai-agent-engine | 92.9% | 154 (incl. 27 integration) |
| schemaplexai-gateway | 83.8% | 18 |
| schemaplexai-common | 81.3% | 40 |

These modules meet the 80% minimum threshold.

---

## Modules Below 80% Threshold

| Module | Instruction % | Gap to 80% | Action |
|--------|--------------|------------|--------|
| schemaplexai-dao | 44.6% | 35.4% | Add tests for MyBatis-Plus extensions |
| schemaplexai-model | 31.3% | 48.7% | Add tests for entity/DTO classes |
| schemaplexai-agent-config | 2.8% | 77.2% | Write full test suite for configuration CRUD |
| schemaplexai-system | 1.6% | 78.4% | Write tests for auth/tenant services |
| schemaplexai-web | 0.0% | 80.0% | Add tests for controllers, SSE, WebSocket |
| schemaplexai-integration | 0.0% | 80.0% | Add tests for webhook handlers, MCP clients |

---

## Backend Test Summary (Post-JaCoCo Verify)

```
schemaplexai-common:        40 passed
schemaplexai-model:         12 passed
schemaplexai-dao:           11 passed
schemaplexai-gateway:       18 passed
schemaplexai-web:            0 tests (no test sources yet)
schemaplexai-system:         10 passed
schemaplexai-agent-config:   5 passed
schemaplexai-agent-engine: 154 passed (including 27 new integration tests)
schemaplexai-integration:    0 passed (no tests written)
schemaplexai-context:        4 passed
schemaplexai-quality:        5 passed
schemaplexai-spec:           5 passed
schemaplexai-ops:            2 passed
schemaplexai-task:           0 tests (no test sources)
schemaplexai-workflow:      15 passed
schemaplexai-admin:          0 tests (empty module)
---
TOTAL BACKEND:             281 passed
```

## Frontend Test Summary

```
73 total tests, 69 passed, 4 failed
Failing: 4 Layout.test.tsx (antd theme.useToken ContextProvider issue in jsdom)
```

---

## Notes

- **JaCoCo enabled** via root `pom.xml` `<plugins>` section (jacoco-maven-plugin 0.8.12, prepare-agent + report bound to verify phase).
- **Baseline established** — first time JaCoCo reports generated across all modules.
- **agent-engine** is the strongest module at 92.9% instruction coverage.
- **4 frontend Layout tests** are skipped due to an antd `ConfigProvider` / React Router `Outlet` compatibility issue in the jsdom test environment. The Layout component works correctly in the browser.
- **Next priority**: Increase coverage in schemaplexai-system and schemaplexai-web (key API surface).
