package com.schemaplexai.agent.engine.tool.sandbox.provider.cube;

import com.schemaplexai.agent.engine.config.CubeSandboxProperties;
import com.schemaplexai.agent.engine.tool.ToolErrorCategory;
import com.schemaplexai.agent.engine.tool.sandbox.SandboxException;
import com.schemaplexai.agent.engine.tool.sandbox.provider.cube.dto.CreateSandboxRequest;
import com.schemaplexai.agent.engine.tool.sandbox.provider.cube.dto.CreateSandboxResponse;
import com.schemaplexai.agent.engine.tool.sandbox.provider.cube.dto.ExecRequest;
import com.schemaplexai.agent.engine.tool.sandbox.provider.cube.dto.ExecResponse;
import com.schemaplexai.agent.engine.tool.sandbox.provider.cube.dto.FileResponse;
import com.schemaplexai.agent.engine.tool.sandbox.provider.cube.dto.SandboxInfoResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.time.Duration;
import java.util.Base64;
import java.util.Map;

/**
 * Low-level REST client for the CubeSandbox E2B-compatible API.
 *
 * <p>Encapsulates all HTTP calls. Not a Spring component — instantiated by
 * {@link CubeSandboxProvider} with configured properties. Package-private
 * constructor overload accepts a mock {@link RestTemplate} for testing.
 */
@Slf4j
public class CubeSandboxClient {

    private final RestTemplate restTemplate;
    private final String apiUrl;
    private final String apiKey;

    public CubeSandboxClient(CubeSandboxProperties properties) {
        this.apiUrl = properties.getApiUrl().replaceAll("/+$", "");
        this.apiKey = properties.getApiKey();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(properties.getRequestTimeoutSeconds()));
        this.restTemplate = new RestTemplate(factory);
    }

    CubeSandboxClient(RestTemplate restTemplate, String apiUrl, String apiKey) {
        this.restTemplate = restTemplate;
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
    }

    /**
     * POST /sandboxes — create a new sandbox.
     */
    public CreateSandboxResponse createSandbox(CreateSandboxRequest request) throws SandboxException {
        try {
            HttpEntity<CreateSandboxRequest> entity = new HttpEntity<>(request, authHeaders());
            ResponseEntity<CreateSandboxResponse> response = restTemplate.postForEntity(
                    apiUrl + "/sandboxes", entity, CreateSandboxResponse.class);
            return response.getBody();
        } catch (Exception e) {
            throw wrapHttpError("createSandbox", e);
        }
    }

    /**
     * POST /sandboxes/{id}/exec — execute a command in the sandbox.
     */
    public ExecResponse exec(String sandboxId, ExecRequest request) throws SandboxException {
        try {
            HttpEntity<ExecRequest> entity = new HttpEntity<>(request, authHeaders());
            ResponseEntity<ExecResponse> response = restTemplate.postForEntity(
                    apiUrl + "/sandboxes/" + sandboxId + "/exec", entity, ExecResponse.class);
            return response.getBody();
        } catch (Exception e) {
            throw wrapHttpError("exec", e);
        }
    }

    /**
     * POST /sandboxes/{id}/files — write a file to the sandbox.
     */
    public void writeFile(String sandboxId, String path, byte[] content) throws SandboxException {
        try {
            HttpHeaders headers = authHeaders();
            Map<String, Object> body = Map.of(
                    "path", path,
                    "content", Base64.getEncoder().encodeToString(content)
            );
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            restTemplate.postForEntity(
                    apiUrl + "/sandboxes/" + sandboxId + "/files", entity, Void.class);
        } catch (Exception e) {
            throw wrapHttpError("writeFile", e);
        }
    }

    /**
     * GET /sandboxes/{id}/files?path=... — read a file from the sandbox.
     */
    public byte[] readFile(String sandboxId, String path) throws SandboxException {
        try {
            HttpEntity<Void> entity = new HttpEntity<>(authHeaders());
            ResponseEntity<FileResponse> response = restTemplate.exchange(
                    apiUrl + "/sandboxes/" + sandboxId + "/files?path=" + path,
                    HttpMethod.GET, entity, FileResponse.class);
            FileResponse body = response.getBody();
            return body != null ? body.content() : new byte[0];
        } catch (Exception e) {
            throw wrapHttpError("readFile", e);
        }
    }

    /**
     * DELETE /sandboxes/{id} — destroy the sandbox.
     */
    public void destroySandbox(String sandboxId) throws SandboxException {
        try {
            HttpEntity<Void> entity = new HttpEntity<>(authHeaders());
            restTemplate.exchange(
                    apiUrl + "/sandboxes/" + sandboxId,
                    HttpMethod.DELETE, entity, Void.class);
        } catch (Exception e) {
            throw wrapHttpError("destroySandbox", e);
        }
    }

    /**
     * POST /sandboxes/{id}/pause — snapshot and pause the sandbox.
     */
    public void pauseSandbox(String sandboxId) throws SandboxException {
        try {
            HttpEntity<Void> entity = new HttpEntity<>(authHeaders());
            restTemplate.postForEntity(
                    apiUrl + "/sandboxes/" + sandboxId + "/pause", entity, Void.class);
        } catch (Exception e) {
            throw wrapHttpError("pauseSandbox", e);
        }
    }

    /**
     * POST /sandboxes/{id}/resume — restore sandbox from snapshot.
     */
    public void resumeSandbox(String sandboxId) throws SandboxException {
        try {
            HttpEntity<Void> entity = new HttpEntity<>(authHeaders());
            restTemplate.postForEntity(
                    apiUrl + "/sandboxes/" + sandboxId + "/resume", entity, Void.class);
        } catch (Exception e) {
            throw wrapHttpError("resumeSandbox", e);
        }
    }

    /**
     * GET /sandboxes/{id} — get sandbox info.
     */
    public SandboxInfoResponse getSandboxInfo(String sandboxId) throws SandboxException {
        try {
            HttpEntity<Void> entity = new HttpEntity<>(authHeaders());
            ResponseEntity<SandboxInfoResponse> response = restTemplate.exchange(
                    apiUrl + "/sandboxes/" + sandboxId,
                    HttpMethod.GET, entity, SandboxInfoResponse.class);
            return response.getBody();
        } catch (Exception e) {
            throw wrapHttpError("getSandboxInfo", e);
        }
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private SandboxException wrapHttpError(String operation, Exception e) {
        if (e instanceof ResourceAccessException rae) {
            return new SandboxException(
                    "CubeSandbox API unreachable during " + operation + ": " + rae.getMessage(),
                    rae, ToolErrorCategory.SANDBOX_ERROR);
        }
        if (e instanceof HttpStatusCodeException hsce) {
            int code = hsce.getStatusCode().value();
            ToolErrorCategory cat = switch (code / 100) {
                case 4 -> code == 429
                        ? ToolErrorCategory.RATE_LIMITED
                        : ToolErrorCategory.INVALID_ARGUMENT;
                default -> ToolErrorCategory.SANDBOX_ERROR;
            };
            return new SandboxException(
                    "CubeSandbox API error during " + operation + ": HTTP " + code
                            + " - " + hsce.getResponseBodyAsString(),
                    hsce, cat);
        }
        return new SandboxException(
                "CubeSandbox API error during " + operation + ": " + e.getMessage(),
                e, ToolErrorCategory.SANDBOX_ERROR);
    }
}
