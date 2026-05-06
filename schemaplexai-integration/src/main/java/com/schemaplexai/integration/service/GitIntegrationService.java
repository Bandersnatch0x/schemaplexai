package com.schemaplexai.integration.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class GitIntegrationService {

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    // In-memory repository metadata store (key: repoId)
    private final Map<Long, Map<String, Object>> repoStore = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> webhookStore = new ConcurrentHashMap<>();
    private long repoIdSequence = 1;

    // --- Repository CRUD ---

    public synchronized Long registerRepository(String provider, String owner, String repoName,
                                                  String cloneUrl, String defaultBranch, String accessToken) {
        if (provider == null || provider.isBlank() || cloneUrl == null || cloneUrl.isBlank()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Provider and clone URL are required");
        }
        long repoId = repoIdSequence++;
        Map<String, Object> repo = new ConcurrentHashMap<>();
        repo.put("id", repoId);
        repo.put("provider", provider.toLowerCase());
        repo.put("owner", owner);
        repo.put("repoName", repoName);
        repo.put("cloneUrl", cloneUrl);
        repo.put("defaultBranch", defaultBranch != null ? defaultBranch : "main");
        repo.put("accessToken", accessToken);
        repo.put("createdAt", Instant.now().toString());
        repo.put("status", "active");
        repoStore.put(repoId, repo);
        log.info("Repository registered: id={}, provider={}, repo={}/{}", repoId, provider, owner, repoName);
        return repoId;
    }

    public Map<String, Object> getRepository(Long repoId) {
        Map<String, Object> repo = repoStore.get(repoId);
        if (repo == null) {
            throw new BaseException(ResultCode.NOT_FOUND, "Repository not found: " + repoId);
        }
        // Return without exposing token
        Map<String, Object> safe = new ConcurrentHashMap<>(repo);
        safe.remove("accessToken");
        return safe;
    }

    public List<Map<String, Object>> listRepositories() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> repo : repoStore.values()) {
            Map<String, Object> safe = new ConcurrentHashMap<>(repo);
            safe.remove("accessToken");
            result.add(safe);
        }
        return result;
    }

    public void deleteRepository(Long repoId) {
        if (repoStore.remove(repoId) == null) {
            throw new BaseException(ResultCode.NOT_FOUND, "Repository not found: " + repoId);
        }
        log.info("Repository deleted: {}", repoId);
    }

    // --- Git Operations ---

    public String cloneRepository(Long repoId, String targetDir) {
        Map<String, Object> repo = repoStore.get(repoId);
        if (repo == null) {
            throw new BaseException(ResultCode.NOT_FOUND, "Repository not found: " + repoId);
        }
        String cloneUrl = (String) repo.get("cloneUrl");
        String accessToken = (String) repo.get("accessToken");
        String authUrl = injectToken(cloneUrl, accessToken);

        Path dest = targetDir != null ? Path.of(targetDir) : Path.of(System.getProperty("java.io.tmpdir"), "git-repos", repoId + "-" + UUID.randomUUID());
        try {
            Files.createDirectories(dest);
            executeGitCommandInDir(dest.getParent().toString(), "clone", authUrl, dest.getFileName().toString());
            log.info("Repository {} cloned to {}", repoId, dest);
            return dest.toString();
        } catch (Exception e) {
            log.error("Failed to clone repository {}", repoId, e);
            throw new BaseException(ResultCode.TOOL_EXECUTION_FAILED, "Clone failed: " + e.getMessage());
        }
    }

    public String pullRepository(Long repoId, String localPath) {
        Map<String, Object> repo = repoStore.get(repoId);
        if (repo == null) {
            throw new BaseException(ResultCode.NOT_FOUND, "Repository not found: " + repoId);
        }
        if (localPath == null || localPath.isBlank()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Local path is required");
        }
        try {
            executeGitCommandInDir(localPath, "pull");
            log.info("Repository {} pulled at {}", repoId, localPath);
            return localPath;
        } catch (Exception e) {
            log.error("Failed to pull repository {}", repoId, e);
            throw new BaseException(ResultCode.TOOL_EXECUTION_FAILED, "Pull failed: " + e.getMessage());
        }
    }

    public String pushRepository(Long repoId, String localPath, String branch) {
        Map<String, Object> repo = repoStore.get(repoId);
        if (repo == null) {
            throw new BaseException(ResultCode.NOT_FOUND, "Repository not found: " + repoId);
        }
        if (localPath == null || localPath.isBlank()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Local path is required");
        }
        String targetBranch = branch != null ? branch : (String) repo.get("defaultBranch");
        try {
            executeGitCommandInDir(localPath, "push", "origin", targetBranch);
            log.info("Repository {} pushed to branch {}", repoId, targetBranch);
            return localPath;
        } catch (Exception e) {
            log.error("Failed to push repository {}", repoId, e);
            throw new BaseException(ResultCode.TOOL_EXECUTION_FAILED, "Push failed: " + e.getMessage());
        }
    }

    // --- Branch Management ---

    public List<String> listBranches(Long repoId, String localPath) {
        if (localPath == null || localPath.isBlank()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Local path is required");
        }
        try {
            String output = executeGitCommandInDir(localPath, "branch", "-a");
            List<String> branches = new ArrayList<>();
            for (String line : output.split("\n")) {
                String trimmed = line.trim().replace("* ", "");
                if (!trimmed.isEmpty()) {
                    branches.add(trimmed);
                }
            }
            return branches;
        } catch (Exception e) {
            log.error("Failed to list branches for repo {}", repoId, e);
            throw new BaseException(ResultCode.TOOL_EXECUTION_FAILED, "List branches failed: " + e.getMessage());
        }
    }

    public void createBranch(Long repoId, String localPath, String branchName, String baseBranch) {
        if (branchName == null || branchName.isBlank()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Branch name is required");
        }
        try {
            if (baseBranch != null && !baseBranch.isBlank()) {
                executeGitCommandInDir(localPath, "checkout", baseBranch);
            }
            executeGitCommandInDir(localPath, "checkout", "-b", branchName);
            log.info("Branch {} created for repo {}", branchName, repoId);
        } catch (Exception e) {
            log.error("Failed to create branch for repo {}", repoId, e);
            throw new BaseException(ResultCode.TOOL_EXECUTION_FAILED, "Create branch failed: " + e.getMessage());
        }
    }

    public void deleteBranch(Long repoId, String localPath, String branchName, boolean force) {
        if (branchName == null || branchName.isBlank()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Branch name is required");
        }
        try {
            String flag = force ? "-D" : "-d";
            executeGitCommandInDir(localPath, "branch", flag, branchName);
            log.info("Branch {} deleted for repo {}", branchName, repoId);
        } catch (Exception e) {
            log.error("Failed to delete branch for repo {}", repoId, e);
            throw new BaseException(ResultCode.TOOL_EXECUTION_FAILED, "Delete branch failed: " + e.getMessage());
        }
    }

    // --- Webhook Handling ---

    public void handleOAuthCallback(String provider, String code) {
        log.info("Handle OAuth callback for provider: {}, code: {}", provider, code);
        // Phase 1: Store authorization code, exchange for access token via provider API
        // Phase 2: Persist token securely for subsequent API calls
        // For now, log the callback and acknowledge receipt
        if (provider == null || provider.isBlank() || code == null || code.isBlank()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Provider and authorization code are required");
        }
        log.info("OAuth callback acknowledged for provider: {}", provider);
    }

    public void handleWebhook(String provider, String payload) {
        log.info("Handle webhook for provider: {}", provider);
        if (provider == null || provider.isBlank() || payload == null || payload.isBlank()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Provider and payload are required");
        }
        try {
            JsonNode root = objectMapper.readTree(payload);
            String eventType = extractEventType(root, provider);
            String repository = extractRepository(root, provider);
            String branch = extractBranch(root, provider);
            String commitSha = extractCommitSha(root, provider);

            String webhookId = UUID.randomUUID().toString();
            Map<String, Object> record = new ConcurrentHashMap<>();
            record.put("id", webhookId);
            record.put("provider", provider);
            record.put("eventType", eventType);
            record.put("repository", repository);
            record.put("branch", branch);
            record.put("commitSha", commitSha);
            record.put("receivedAt", Instant.now().toString());
            record.put("payload", payload);
            webhookStore.put(webhookId, record);

            log.info("Webhook {} received: event={}, repo={}, branch={}", webhookId, eventType, repository, branch);

            // Phase 2: Trigger workflow or agent based on event type
            processWebhookEvent(eventType, repository, branch, commitSha);
        } catch (Exception e) {
            log.error("Failed to parse webhook payload", e);
            throw new BaseException(ResultCode.PARAM_ERROR, "Invalid webhook payload: " + e.getMessage());
        }
    }

    public List<Map<String, Object>> listWebhookEvents(String repository, String eventType, int limit) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> event : webhookStore.values()) {
            if (repository != null && !repository.equals(event.get("repository"))) {
                continue;
            }
            if (eventType != null && !eventType.equals(event.get("eventType"))) {
                continue;
            }
            result.add(event);
            if (result.size() >= limit) {
                break;
            }
        }
        return result;
    }

    // --- GitHub/GitLab REST API helpers ---

    public String fetchRepositoryInfo(Long repoId) {
        Map<String, Object> repo = repoStore.get(repoId);
        if (repo == null) {
            throw new BaseException(ResultCode.NOT_FOUND, "Repository not found: " + repoId);
        }
        String provider = (String) repo.get("provider");
        String owner = (String) repo.get("owner");
        String repoName = (String) repo.get("repoName");
        String accessToken = (String) repo.get("accessToken");

        if ("github".equals(provider)) {
            return callGitHubApi("/repos/" + owner + "/" + repoName, accessToken);
        } else if ("gitlab".equals(provider)) {
            return callGitLabApi("/projects/" + owner + "%2F" + repoName, accessToken);
        }
        throw new BaseException(ResultCode.PARAM_ERROR, "Unsupported provider: " + provider);
    }

    public String fetchBranchesViaApi(Long repoId) {
        Map<String, Object> repo = repoStore.get(repoId);
        if (repo == null) {
            throw new BaseException(ResultCode.NOT_FOUND, "Repository not found: " + repoId);
        }
        String provider = (String) repo.get("provider");
        String owner = (String) repo.get("owner");
        String repoName = (String) repo.get("repoName");
        String accessToken = (String) repo.get("accessToken");

        if ("github".equals(provider)) {
            return callGitHubApi("/repos/" + owner + "/" + repoName + "/branches", accessToken);
        } else if ("gitlab".equals(provider)) {
            return callGitLabApi("/projects/" + owner + "%2F" + repoName + "/repository/branches", accessToken);
        }
        throw new BaseException(ResultCode.PARAM_ERROR, "Unsupported provider: " + provider);
    }

    // --- Private helpers ---

    private String injectToken(String cloneUrl, String token) {
        if (token == null || token.isBlank()) {
            return cloneUrl;
        }
        if (cloneUrl.startsWith("https://")) {
            return cloneUrl.replaceFirst("https://", "https://" + token + "@");
        }
        return cloneUrl;
    }

    private String executeGitCommandInDir(String workingDir, String... args) throws Exception {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.addAll(List.of(args));
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(new File(workingDir));
        pb.redirectErrorStream(true);
        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Git command failed with exit code " + exitCode + ": " + output);
        }
        return output.toString();
    }

    private String extractEventType(JsonNode root, String provider) {
        return switch (provider.toLowerCase()) {
            case "github" -> root.path("action").asText(root.path("event").asText("unknown"));
            case "gitlab" -> root.path("object_kind").asText("unknown");
            default -> root.path("event_type").asText("unknown");
        };
    }

    private String extractRepository(JsonNode root, String provider) {
        return switch (provider.toLowerCase()) {
            case "github" -> root.path("repository").path("full_name").asText("unknown");
            case "gitlab" -> root.path("project").path("path_with_namespace").asText("unknown");
            default -> root.path("repository").path("full_name").asText("unknown");
        };
    }

    private String extractBranch(JsonNode root, String provider) {
        return switch (provider.toLowerCase()) {
            case "github" -> root.path("ref").asText("unknown").replace("refs/heads/", "");
            case "gitlab" -> root.path("ref").asText("unknown").replace("refs/heads/", "");
            default -> root.path("ref").asText("unknown").replace("refs/heads/", "");
        };
    }

    private String extractCommitSha(JsonNode root, String provider) {
        return switch (provider.toLowerCase()) {
            case "github" -> root.path("after").asText(root.path("head_commit").path("id").asText("unknown"));
            case "gitlab" -> root.path("after").asText(root.path("checkout_sha").asText("unknown"));
            default -> root.path("after").asText("unknown");
        };
    }

    private void processWebhookEvent(String eventType, String repository, String branch, String commitSha) {
        log.info("Processing webhook event: type={}, repo={}, branch={}, commit={}",
                eventType, repository, branch, commitSha);
        // TODO: integrate with workflow engine or agent engine to trigger downstream actions
    }

    private String callGitHubApi(String path, String token) {
        String url = "https://api.github.com" + path;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/vnd.github+json");
        if (token != null && !token.isBlank()) {
            headers.set("Authorization", "Bearer " + token);
        }
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
        return response.getBody();
    }

    private String callGitLabApi(String path, String token) {
        String url = "https://gitlab.com/api/v4" + path;
        HttpHeaders headers = new HttpHeaders();
        if (token != null && !token.isBlank()) {
            headers.set("PRIVATE-TOKEN", token);
        }
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
        return response.getBody();
    }
}
