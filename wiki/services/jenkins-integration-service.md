---
title: JenkinsIntegrationService
type: service
source: schemaplexai-integration/src/main/java/com/schemaplexai/integration/service/JenkinsIntegrationService.java
creation_date: 2026-05-07
update_date: 2026-05-07
tags: [service, integration, jenkins, ci-cd, build]
confidence: high
---

# JenkinsIntegrationService

> One-sentence summary: Integrates with Jenkins CI/CD servers to trigger builds, monitor status, and handle build callbacks.

## Responsibilities

1. Trigger Jenkins builds with optional parameters via REST API
2. Handle asynchronous build result callbacks
3. Query build status (specific build or latest) from Jenkins
4. List all Jenkins jobs with last build summary
5. Cache build status in memory for quick lookup
6. Handle Jenkins unreachability gracefully

## Key Methods

| Method | Description | Parameters | Return |
|--------|-------------|------------|--------|
| `triggerBuild` | Trigger a Jenkins build with parameters | `jenkinsUrl` (String), `jobName` (String), `username` (String), `apiToken` (String), `parameters` (Map<String, String>) | `void` |
| `handleBuildCallback` | Process a build result callback from Jenkins | `jobName` (String), `buildResult` (String) | `void` |
| `getBuildStatus` | Query build status from Jenkins API | `jenkinsUrl` (String), `jobName` (String), `buildNumber` (Integer), `username` (String), `apiToken` (String) | `Map<String, Object>` |
| `listJobs` | List all jobs from a Jenkins instance | `jenkinsUrl` (String), `username` (String), `apiToken` (String) | `List<Map<String, Object>>` |

## Dependencies / Collaborators

- **RestTemplate** — HTTP client for Jenkins REST API calls
- **ObjectMapper** — JSON parsing for Jenkins API responses
- **In-memory cache**: `ConcurrentHashMap` keyed by `jobName#buildNumber`

## Known Issues

- **Build callback processing** — `processBuildResult` is a stub; workflow engine / agent engine integration is TODO
- **Parameter encoding** — build parameters are URL-encoded manually; special characters may need additional handling
- **No persistent build history** — build status is cached in memory only

## Related

- [[services/git-integration-service]] — VCS integration counterpart
- [[services/tool-execution-service]] — may trigger Jenkins builds as tools
- [[services/integration-service]] — broader integration management
