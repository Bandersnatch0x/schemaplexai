# Run-Tasks Doc Impact Report — 2026-05-08

## Run-Tasks Report
- Mode used: 3
- Reason: 2 groups of shared-file collisions (docker-compose.yml, ci.yml) formed partial dependency graph
- Tasks total: 10
- Completed: 10
- Failed / skipped: 0
- Next suggested action: Run `mvn clean test` + `npm run test:run` to verify all changes; update wiki/deployment.md with new mandatory env vars

## Aggregated Doc Impact

### Files touched
- `docker/docker-compose.yml`
- `.github/workflows/ci.yml`
- `.gitignore`
- `schemaplexai-gateway/src/main/resources/application.yml`
- `schemaplexai-admin/src/main/resources/application.yml`
- `schemaplexai-agent-config/src/main/resources/application.yml`
- `schemaplexai-agent-engine/src/main/resources/application.yml`
- `schemaplexai-context/src/main/resources/application.yml`
- `schemaplexai-spec/src/main/resources/application.yml`
- `schemaplexai-workflow/src/main/resources/application.yml`
- `schemaplexai-ops/src/main/resources/application.yml`
- `schemaplexai-task/src/main/resources/application.yml`
- `schemaplexai-system/src/main/resources/application.yml`
- `schemaplexai-task/src/main/java/.../NotificationConsumer.java`
- `schemaplexai-ops/src/main/java/.../CostService.java`
- `schemaplexai-ui/e2e/smoke.spec.ts`
- `schemaplexai-agent-engine/src/test/.../AgentExecutionControllerSseIntegrationTest.java`
- `schemaplexai-task/src/test/.../NotificationConsumerTest.java`
- `schemaplexai-ops/src/test/.../CostServiceTest.java`

### Wiki sections needing sync
- `wiki/dependencies.md` — document new required env vars (POSTGRES_PASSWORD, RABBITMQ_DEFAULT_PASS, MINIO_ROOT_PASSWORD, GRAFANA_ADMIN_PASSWORD)
- `wiki/security.md` — note CORS restriction to explicit origins
- `wiki/testing.md` — e2e/smoke.spec.ts is canonical smoke entrypoint
- `wiki/services/clickhouse-cost-schema.md` — v1 uses PG sf_budget.used_amount; ClickHouse deferred to v2
- `wiki/services/notification.md` — v1 in-app-only; non-in-app channels routed to DLQ
- `wiki/gaps.md` — remove P0-1, P0-3, P0-5, P1-4 gap items if present

### Decisions to log
1. All Docker infrastructure ports bound to 127.0.0.1 by default; plaintext credentials removed from docker-compose.yml in favor of fail-fast env vars.
2. Migrated all application.yml credential defaults from weak hardcoded values to Spring Boot `${VAR:?required}` fail-fast pattern; tightened Gateway CORS from wildcard origin to explicit localhost allowlist.
3. CI now enforces Trivy (CRITICAL/HIGH), OWASP Dependency-Check (CVSS 9+), and Gitleaks as required pre-merge gates; Dependency-Check set to `continue-on-error: true` for initial adoption.
4. v1 cost analytics uses PG short-path via `sf_budget.used_amount` aggregation; ClickHouse `sf_cost_record` deferred to v2.
5. Notification delivery reduced to in-app only; non-in-app channels (email/sms/webhook) silently routed to DLQ.

### Risk flags
- **BREAKING**: docker-compose ports bound to 127.0.0.1 — remote/WSL2 external IP access requires tunnel or internal DNS
- **BREAKING**: DB_PASSWORD, DB_USERNAME, RABBITMQ_PASSWORD, RABBITMQ_USERNAME, CLICKHOUSE_PASSWORD, CLICKHOUSE_USERNAME now mandatory with no defaults; all envs must set these vars
- **BREAKING**: Gateway CORS only allows localhost:5173 and localhost:3000; custom domains or deployed previews blocked
- **APPROXIMATION**: CostService.todayCost and monthCost mirror totalCost in v1 (not true time-bounded costs until v2 ClickHouse)
- **SILENT DROP**: Notification email/SMS/webhook channels are silently dropped to DLQ — downstream systems expecting these will stop receiving notifications
