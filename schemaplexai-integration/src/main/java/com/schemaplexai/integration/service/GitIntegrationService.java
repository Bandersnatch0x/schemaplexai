package com.schemaplexai.integration.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.integration.config.IntegrationOAuthProperties;
import com.schemaplexai.integration.security.IntegrationCredentialEncryptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
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
    private final IntegrationCredentialEncryptor credentialEncryptor;
    private final IntegrationOAuthProperties oauthProperties;

    // In-memory repository metadata store (key: repoId).
    // Access tokens are stored ONLY as AES-256-GCM ciphertext ("accessTokenCipher");
    // plaintext tokens never reside in this store.
    private final Map<Long, Map<String, Object>> repoStore = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Object>> webhookStore = new ConcurrentHashMap<>();
    // OAuth access tokens per tenant/provider — ciphertext only (key: tenantId -> provider).
    private final Map<Long, Map<String, Map<String, Object>>> oauthTokenStore = new ConcurrentHashMap<>();
    private long repoIdSequence = 1;

    // --- Repository CRUD ---

    public synchronized Long registerRepository(Long tenantId, String provider, String owner, String repoName,
                                                  String cloneUrl, String defaultBranch, String accessToken) {
        if (tenantId == null) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Tenant ID is required");
        }
        if (provider == null || provider.isBlank() || cloneUrl == null || cloneUrl.isBlank()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Provider and clone URL are required");
        }
        long repoId = repoIdSequence++;
        Map<String, Object> repo = new ConcurrentHashMap<>();
        repo.put("id", repoId);
        repo.put("tenantId", tenantId);
        repo.put("provider", provider.toLowerCase());
        repo.put("owner", owner);
        repo.put("repoName", repoName);
        repo.put("cloneUrl", cloneUrl);
        repo.put("defaultBranch", defaultBranch != null ? defaultBranch : "main");
        if (accessToken != null && !accessToken.isBlank()) {
            // Encrypt with the tenant-scoped key; plaintext is confined to this call frame.
            repo.put("accessTokenCipher", credentialEncryptor.encrypt(accessToken, tenantId));
        }
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
        // Return without exposing any credential material (ciphertext included)
        Map<String, Object> safe = new ConcurrentHashMap<>(repo);
        safe.remove("accessTokenCipher");
        return safe;
    }

    public List<Map<String, Object>> listRepositories() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> repo : repoStore.values()) {
            Map<String, Object> safe = new ConcurrentHashMap<>(repo);
            safe.remove("accessTokenCipher");
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

    /** Public helper for test isolation. */
    public void clearStore() {
        repoStore.clear();
        webhookStore.clear();
        oauthTokenStore.clear();
        repoIdSequence = 1;
    }

    // --- Git Operations ---

    public String cloneRepository(Long repoId, String targetDir) {
        Map<String, Object> repo = repoStore.get(repoId);
        if (repo == null) {
            throw new BaseException(ResultCode.NOT_FOUND, "Repository not found: " + repoId);
        }
        String cloneUrl = (String) repo.get("cloneUrl");

        Path dest = targetDir != null ? Path.of(targetDir) : Path.of(System.getProperty("java.io.tmpdir"), "git-repos", repoId + "-" + UUID.randomUUID());
        try {
            Files.createDirectories(dest);
            List<String> cloneArgs = buildCloneArguments(repo, cloneUrl, dest.getFileName().toString());
            executeGitCommandInDir(dest.getParent().toString(), cloneArgs);
            log.info("Repository {} cloned to {}", repoId, dest);
            return dest.toString();
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to clone repository {}", repoId, e);
            throw new BaseException(ResultCode.TOOL_EXECUTION_FAILED, "Clone failed: " + e.getMessage());
        }
    }

    /**
     * Build the git clone argument list with credential injection.
     *
     * <p>The stored ciphertext is decrypted only here, at the moment of use, and the
     * token is injected through git's {@code http.extraHeader} configuration (Basic
     * auth header) instead of being embedded in the clone URL. This keeps the URL —
     * which git persists as the {@code origin} remote and may surface in logs and
     * error messages — free of any credential material.
     */
    List<String> buildCloneArguments(Map<String, Object> repo, String cloneUrl, String destName) {
        List<String> args = new ArrayList<>();
        String token = resolveAccessToken(repo);
        if (token != null && !token.isBlank()) {
            String provider = String.valueOf(repo.getOrDefault("provider", ""));
            String basicAuth = basicAuthUsername(provider) + ":" + token;
            String header = "Authorization: Basic "
                    + Base64.getEncoder().encodeToString(basicAuth.getBytes(StandardCharsets.UTF_8));
            args.add("-c");
            args.add("http.extraHeader=" + header);
        }
        args.add("clone");
        args.add(cloneUrl);
        args.add(destName);
        return args;
    }

    /** Username portion of HTTP Basic auth per provider (tokens act as the password). */
    private String basicAuthUsername(String provider) {
        return switch (provider) {
            case "github" -> "x-access-token";
            case "gitlab" -> "oauth2";
            default -> "git";
        };
    }

    /**
     * Decrypt the stored access token on demand. The plaintext exists only within
     * the calling frame and is never persisted or logged.
     */
    private String resolveAccessToken(Map<String, Object> repo) {
        Object cipher = repo.get("accessTokenCipher");
        if (cipher == null) {
            return null;
        }
        Long tenantId = (Long) repo.get("tenantId");
        return credentialEncryptor.decrypt((String) cipher, tenantId);
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

    /**
     * Handle the OAuth authorization callback: exchange the authorization code for
     * an access token at the provider's token endpoint, then persist the token
     * encrypted (AES-256-GCM, tenant-scoped key). The plaintext token exists only
     * inside this call frame; failures degrade to structured {@link BaseException}s
     * and nothing is stored.
     *
     * @return connection status (provider, status, connectedAt) — never token material
     */
    public Map<String, Object> handleOAuthCallback(Long tenantId, String provider, String code) {
        log.info("Handle OAuth callback for provider: {}", provider);
        if (tenantId == null) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Tenant ID is required");
        }
        if (provider == null || provider.isBlank() || code == null || code.isBlank()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Provider and authorization code are required");
        }

        String providerKey = provider.toLowerCase();
        IntegrationOAuthProperties.Provider providerConfig = oauthProperties.provider(providerKey);
        if (providerConfig == null
                || providerConfig.getClientId() == null || providerConfig.getClientId().isBlank()
                || providerConfig.getClientSecret() == null || providerConfig.getClientSecret().isBlank()
                || providerConfig.getTokenUrl() == null || providerConfig.getTokenUrl().isBlank()) {
            log.warn("OAuth callback received for unconfigured provider: {}", providerKey);
            throw new BaseException(ResultCode.ERROR, "OAuth is not configured for provider: " + providerKey);
        }

        String accessToken = exchangeAuthorizationCode(providerConfig, providerKey, code);

        // Encrypt before persisting; only ciphertext enters the store.
        Map<String, Object> record = new ConcurrentHashMap<>();
        record.put("provider", providerKey);
        record.put("tokenCipher", credentialEncryptor.encrypt(accessToken, tenantId));
        record.put("status", "connected");
        record.put("connectedAt", Instant.now().toString());
        oauthTokenStore.computeIfAbsent(tenantId, k -> new ConcurrentHashMap<>()).put(providerKey, record);
        log.info("OAuth access token stored (encrypted) for provider: {}", providerKey);

        Map<String, Object> result = new ConcurrentHashMap<>();
        result.put("provider", providerKey);
        result.put("status", "connected");
        result.put("connectedAt", record.get("connectedAt"));
        return result;
    }

    /**
     * Connection status + stored ciphertext for a tenant/provider, or null when the
     * tenant has no OAuth connection for that provider. Plaintext is never returned.
     */
    public Map<String, Object> getOAuthConnection(Long tenantId, String provider) {
        if (tenantId == null || provider == null) {
            return null;
        }
        Map<String, Map<String, Object>> byProvider = oauthTokenStore.get(tenantId);
        if (byProvider == null) {
            return null;
        }
        Map<String, Object> record = byProvider.get(provider.toLowerCase());
        return record != null ? new ConcurrentHashMap<>(record) : null;
    }

    /** Exchange the authorization code for an access token at the provider endpoint. */
    private String exchangeAuthorizationCode(IntegrationOAuthProperties.Provider config,
                                             String providerKey, String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Accept", "application/json");

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("client_id", config.getClientId());
        form.add("client_secret", config.getClientSecret());
        if (config.getRedirectUri() != null && !config.getRedirectUri().isBlank()) {
            form.add("redirect_uri", config.getRedirectUri());
        }

        try {
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(form, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    config.getTokenUrl(), HttpMethod.POST, request, String.class);

            JsonNode root = objectMapper.readTree(response.getBody() != null ? response.getBody() : "{}");
            if (root.hasNonNull("error")) {
                String description = root.path("error_description").asText(root.path("error").asText());
                log.warn("OAuth token exchange rejected by provider {}: {}", providerKey, description);
                throw new BaseException(ResultCode.TOOL_EXECUTION_FAILED,
                        "OAuth token exchange rejected: " + description);
            }
            String accessToken = root.path("access_token").asText("");
            if (accessToken.isBlank()) {
                log.warn("OAuth token exchange returned no access_token for provider {}", providerKey);
                throw new BaseException(ResultCode.TOOL_EXECUTION_FAILED,
                        "OAuth token exchange returned no access token");
            }
            return accessToken;
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("OAuth token exchange failed for provider {}", providerKey, e);
            throw new BaseException(ResultCode.TOOL_EXECUTION_FAILED,
                    "OAuth token exchange failed: " + e.getMessage());
        }
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
            validateWebhookRepository(repository);

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
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse webhook payload", e);
            throw new BaseException(ResultCode.PARAM_ERROR, "Invalid webhook payload: " + e.getMessage());
        }
    }

    public List<Map<String, Object>> listWebhookEvents(String repository, String eventType, int limit) {
        if (limit <= 0) {
            return List.of();
        }
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
        String accessToken = resolveAccessToken(repo);

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
        String accessToken = resolveAccessToken(repo);

        if ("github".equals(provider)) {
            return callGitHubApi("/repos/" + owner + "/" + repoName + "/branches", accessToken);
        } else if ("gitlab".equals(provider)) {
            return callGitLabApi("/projects/" + owner + "%2F" + repoName + "/repository/branches", accessToken);
        }
        throw new BaseException(ResultCode.PARAM_ERROR, "Unsupported provider: " + provider);
    }

    // --- Private helpers ---

    String executeGitCommandInDir(String workingDir, String... args) throws Exception {
        return executeGitCommandInDir(workingDir, List.of(args));
    }

    String executeGitCommandInDir(String workingDir, List<String> args) throws Exception {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.addAll(args);
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
            throw new RuntimeException("Git command failed with exit code " + exitCode);
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

    private void validateWebhookRepository(String repository) {
        if (repository == null || repository.isBlank() || "unknown".equals(repository)) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Webhook repository is required");
        }
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
