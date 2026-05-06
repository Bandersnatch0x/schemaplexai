---
title: GitIntegrationService
type: service
source: schemaplexai-integration/src/main/java/com/schemaplexai/integration/service/GitIntegrationService.java
creation_date: 2026-05-07
update_date: 2026-05-07
tags: [service, integration, git, github, gitlab, webhook, vcs]
confidence: high
---

# GitIntegrationService

> One-sentence summary: Provides Git repository management, local git operations, webhook handling, and REST API integration for GitHub and GitLab.

## Responsibilities

1. Register and manage Git repository metadata (provider, owner, clone URL, access tokens)
2. Execute local git commands: clone, pull, push, branch management
3. Handle OAuth callbacks from Git providers
4. Receive and parse webhook payloads from GitHub/GitLab
5. Query repository information via provider REST APIs
6. Safely mask access tokens in responses

## Key Methods

| Method | Description | Parameters | Return |
|--------|-------------|------------|--------|
| `registerRepository` | Register a new repository with metadata and access token | `provider` (String), `owner` (String), `repoName` (String), `cloneUrl` (String), `defaultBranch` (String), `accessToken` (String) | `Long` (repo ID) |
| `getRepository` | Retrieve repository metadata (token masked) | `repoId` (Long) | `Map<String, Object>` |
| `listRepositories` | List all registered repositories (tokens masked) | — | `List<Map<String, Object>>` |
| `deleteRepository` | Remove a repository from the store | `repoId` (Long) | `void` |
| `cloneRepository` | Clone a repository to a local directory | `repoId` (Long), `targetDir` (String) | `String` (destination path) |
| `pullRepository` | Pull latest changes for a local repository | `repoId` (Long), `localPath` (String) | `String` (local path) |
| `pushRepository` | Push local changes to remote | `repoId` (Long), `localPath` (String), `branch` (String) | `String` (local path) |
| `listBranches` | List all branches in a local repository | `repoId` (Long), `localPath` (String) | `List<String>` |
| `createBranch` | Create a new branch from a base branch | `repoId` (Long), `localPath` (String), `branchName` (String), `baseBranch` (String) | `void` |
| `deleteBranch` | Delete a branch (with optional force) | `repoId` (Long), `localPath` (String), `branchName` (String), `force` (boolean) | `void` |
| `handleOAuthCallback` | Handle OAuth authorization code callback | `provider` (String), `code` (String) | `void` |
| `handleWebhook` | Parse and store incoming webhook payload | `provider` (String), `payload` (String) | `void` |
| `listWebhookEvents` | Query stored webhook events with filters | `repository` (String), `eventType` (String), `limit` (int) | `List<Map<String, Object>>` |
| `fetchRepositoryInfo` | Fetch repository metadata via provider API | `repoId` (Long) | `String` (JSON response) |
| `fetchBranchesViaApi` | Fetch branch list via provider API | `repoId` (Long) | `String` (JSON response) |

## Dependencies / Collaborators

- **ObjectMapper** — JSON parsing for webhook payloads
- **RestTemplate** — HTTP client for GitHub/GitLab REST APIs
- **In-memory stores**: `ConcurrentHashMap` for repo metadata and webhook events (Phase 1)

## Known Issues

- **In-memory storage** — repository and webhook data is stored in `ConcurrentHashMap`; persistence to database is planned for Phase 2
- **OAuth token exchange** — `handleOAuthCallback` logs but does not yet exchange authorization codes for access tokens
- **Webhook event processing** — `processWebhookEvent` is a stub; downstream workflow/agent integration is TODO

## Related

- [[services/jenkins-integration-service]] — CI/CD integration counterpart
- [[services/tool-execution-service]] — may trigger git operations as tools
- [[services/integration-service]] — broader integration management
