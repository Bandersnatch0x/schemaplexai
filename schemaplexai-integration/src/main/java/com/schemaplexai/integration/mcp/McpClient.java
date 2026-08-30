package com.schemaplexai.integration.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.integration.dto.McpToolSchema;
import com.schemaplexai.integration.mcp.McpClientException.FailureKind;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Real Model Context Protocol (MCP) client speaking JSON-RPC 2.0 over the
 * Streamable HTTP transport (single POST endpoint).
 * <p>
 * Issue 930: replaces the legacy stub that always returned an empty tool list.
 * Protocol flow per MCP spec:
 * <ol>
 *   <li>{@code initialize} request/response handshake (the {@code Mcp-Session-Id}
 *       response header is captured and echoed on every subsequent request)</li>
 *   <li>{@code notifications/initialized} notification</li>
 *   <li>{@code tools/list} (paginated via {@code cursor}/{@code nextCursor})
 *       or {@code tools/call}</li>
 * </ol>
 * <p>
 * All HTTP traffic goes through the shared module {@link RestTemplate}, whose
 * connect/read budget defaults to 30s (issue 918, SPEC-INT §7).
 * <p>
 * Failure semantics: any transport, HTTP or protocol failure raises a
 * {@link McpClientException} with a structured {@link FailureKind}. A returned
 * empty list always means the reachable server genuinely exposes no tools —
 * never a silently swallowed error.
 * <p>
 * Thread-safety: state transitions (session initialization) are synchronized;
 * the session id is volatile for safe cross-thread reads.
 */
@Slf4j
public class McpClient {

    /** MCP protocol version negotiated during the initialize handshake. */
    static final String PROTOCOL_VERSION = "2024-11-05";

    /** Safety bound on tools/list pagination round-trips. */
    private static final int MAX_LIST_PAGES = 20;

    /** Header used by the Streamable HTTP transport to bind a session. */
    private static final String SESSION_HEADER = "Mcp-Session-Id";

    private final String endpoint;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final AtomicLong idSequence = new AtomicLong();

    private volatile String sessionId;
    private boolean initialized;

    public McpClient(String endpoint, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.endpoint = Objects.requireNonNull(endpoint, "endpoint must not be null");
        this.restTemplate = Objects.requireNonNull(restTemplate, "restTemplate must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    public String getEndpoint() {
        return endpoint;
    }

    public boolean isInitialized() {
        return initialized;
    }

    /**
     * List all tools exposed by the server, following {@code nextCursor}
     * pagination until exhausted (bounded by {@value #MAX_LIST_PAGES} pages).
     *
     * @return tools exposed by the server; empty only when the server truly
     *         exposes none
     * @throws McpClientException structured failure when the server cannot be
     *         reached, times out, answers non-2xx or speaks invalid JSON-RPC
     */
    public synchronized List<McpToolSchema> listTools() {
        ensureInitialized();

        List<McpToolSchema> tools = new ArrayList<>();
        String cursor = null;
        for (int page = 0; page < MAX_LIST_PAGES; page++) {
            Map<String, Object> params = new LinkedHashMap<>();
            if (cursor != null) {
                params.put("cursor", cursor);
            }
            JsonNode result = request("tools/list", params);

            JsonNode toolsNode = result.path("tools");
            if (toolsNode.isArray()) {
                for (JsonNode toolNode : toolsNode) {
                    McpToolSchema tool = new McpToolSchema();
                    tool.setName(toolNode.path("name").asText(null));
                    tool.setDescription(toolNode.path("description").asText(null));
                    JsonNode inputSchema = toolNode.path("inputSchema");
                    if (!inputSchema.isMissingNode() && inputSchema.isObject()) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> schema = objectMapper.convertValue(inputSchema, Map.class);
                        tool.setInputSchema(schema);
                    }
                    tools.add(tool);
                }
            }

            JsonNode nextCursor = result.path("nextCursor");
            if (nextCursor.isMissingNode() || nextCursor.isNull() || nextCursor.asText("").isEmpty()) {
                return tools;
            }
            cursor = nextCursor.asText();
        }
        log.warn("MCP tools/list pagination stopped at {} pages for endpoint {}", MAX_LIST_PAGES, endpoint);
        return tools;
    }

    /**
     * Invoke a tool on the server via {@code tools/call}.
     *
     * @param name      tool name as advertised by {@code tools/list}
     * @param arguments invocation arguments (may be null for none)
     * @return the JSON-RPC {@code result} node serialized as JSON
     * @throws McpClientException structured failure on transport/HTTP/protocol
     *         errors, or when the server answers with a JSON-RPC error object
     */
    public synchronized String callTool(String name, Map<String, Object> arguments) {
        ensureInitialized();

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("name", name);
        params.put("arguments", arguments != null ? arguments : Map.of());
        JsonNode result = request("tools/call", params);
        return result.toString();
    }

    // ── protocol internals ────────────────────────────────────────────

    private void ensureInitialized() {
        if (initialized) {
            return;
        }

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("protocolVersion", PROTOCOL_VERSION);
        params.put("capabilities", Map.of());
        params.put("clientInfo", Map.of(
                "name", "schemaplexai-integration",
                "version", "1.0.0"));

        JsonNode result = request("initialize", params);
        if (result.path("protocolVersion").isMissingNode() && result.path("serverInfo").isMissingNode()) {
            throw new McpClientException(FailureKind.PROTOCOL_ERROR, endpoint,
                    "MCP initialize response lacks protocolVersion/serverInfo from " + endpoint);
        }

        // Completion notification (no id ⇒ no JSON-RPC response body expected;
        // servers typically answer 202 Accepted).
        send(buildMessage("notifications/initialized", null, null), false);
        initialized = true;
        log.info("MCP session established with {} (session: {})", endpoint,
                sessionId != null ? sessionId : "none");
    }

    /**
     * Send a JSON-RPC request (with id) and return its {@code result} node.
     * A JSON-RPC error object is surfaced as a structured PROTOCOL_ERROR.
     */
    private JsonNode request(String method, Map<String, Object> params) {
        long id = idSequence.incrementAndGet();
        JsonNode root = send(buildMessage(method, params, id), true);

        JsonNode error = root.path("error");
        if (!error.isMissingNode() && !error.isNull()) {
            String message = error.path("message").asText("unknown MCP error");
            throw new McpClientException(FailureKind.PROTOCOL_ERROR, endpoint,
                    "MCP " + method + " failed on " + endpoint + ": " + message);
        }
        JsonNode result = root.path("result");
        if (result.isMissingNode() || result.isNull()) {
            throw new McpClientException(FailureKind.PROTOCOL_ERROR, endpoint,
                    "MCP " + method + " response from " + endpoint + " carries no result");
        }
        return result;
    }

    private Map<String, Object> buildMessage(String method, Map<String, Object> params, Long id) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("jsonrpc", "2.0");
        message.put("method", method);
        if (params != null) {
            message.put("params", params);
        }
        if (id != null) {
            message.put("id", id);
        }
        return message;
    }

    /**
     * POST one JSON-RPC message to the endpoint and parse the response body
     * (plain JSON, or the first {@code data:} frame of an SSE response).
     * Returns null for notifications, which carry no response payload.
     */
    private JsonNode send(Map<String, Object> message, boolean expectResponse) {
        String body;
        try {
            body = objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            throw new McpClientException(FailureKind.PROTOCOL_ERROR, endpoint,
                    "Failed to serialize MCP message for " + endpoint, e);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.ACCEPT, "application/json, text/event-stream");
        if (sessionId != null) {
            headers.set(SESSION_HEADER, sessionId);
        }

        ResponseEntity<String> response;
        try {
            response = restTemplate.exchange(endpoint, HttpMethod.POST,
                    new HttpEntity<>(body, headers), String.class);
        } catch (ResourceAccessException e) {
            FailureKind kind = hasTimeoutCause(e) ? FailureKind.TIMEOUT : FailureKind.UNREACHABLE;
            throw new McpClientException(kind, endpoint,
                    "MCP endpoint " + endpoint + " is "
                            + (kind == FailureKind.TIMEOUT ? "too slow (30s budget exceeded)" : "unreachable")
                            + ": " + rootMessage(e), e);
        } catch (HttpStatusCodeException e) {
            throw new McpClientException(FailureKind.HTTP_ERROR, endpoint,
                    "MCP endpoint " + endpoint + " answered HTTP " + e.getStatusCode().value()
                            + " for " + message.get("method"), e);
        } catch (RestClientException e) {
            throw new McpClientException(FailureKind.PROTOCOL_ERROR, endpoint,
                    "MCP transport failure talking to " + endpoint + ": " + e.getMessage(), e);
        }

        if (sessionId == null) {
            String headerSession = response.getHeaders().getFirst(SESSION_HEADER);
            if (headerSession != null && !headerSession.isBlank()) {
                sessionId = headerSession;
            }
        }

        if (!expectResponse) {
            return null;
        }

        String responseBody = response.getBody();
        if (responseBody == null || responseBody.isBlank()) {
            throw new McpClientException(FailureKind.PROTOCOL_ERROR, endpoint,
                    "MCP endpoint " + endpoint + " returned an empty body for " + message.get("method"));
        }
        return parseJsonRpcBody(responseBody, message.get("method"));
    }

    /**
     * Parse a response body that is either a plain JSON-RPC message or an SSE
     * stream whose {@code data:} frames carry JSON-RPC messages.
     */
    private JsonNode parseJsonRpcBody(String responseBody, Object method) {
        String candidate = responseBody.strip();
        if (!candidate.startsWith("{")) {
            candidate = extractFirstDataFrame(responseBody);
        }
        try {
            JsonNode root = objectMapper.readTree(candidate);
            if (!root.path("jsonrpc").isMissingNode()) {
                return root;
            }
            throw new McpClientException(FailureKind.PROTOCOL_ERROR, endpoint,
                    "MCP endpoint " + endpoint + " returned a non-JSON-RPC body for " + method);
        } catch (McpClientException e) {
            throw e;
        } catch (Exception e) {
            throw new McpClientException(FailureKind.PROTOCOL_ERROR, endpoint,
                    "MCP endpoint " + endpoint + " returned an unparseable body for " + method, e);
        }
    }

    private String extractFirstDataFrame(String sseBody) {
        for (String line : sseBody.split("\n")) {
            String trimmed = line.strip();
            if (trimmed.startsWith("data:")) {
                String data = trimmed.substring("data:".length()).strip();
                if (!data.isEmpty() && !"[DONE]".equals(data)) {
                    return data;
                }
            }
        }
        return sseBody;
    }

    private static boolean hasTimeoutCause(Throwable t) {
        Throwable current = t;
        while (current != null) {
            if (current instanceof SocketTimeoutException) {
                return true;
            }
            current = current.getCause() == current ? null : current.getCause();
        }
        return false;
    }

    private static String rootMessage(Throwable t) {
        Throwable current = t;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current.getMessage() != null ? current.getMessage() : current.getClass().getSimpleName();
    }
}
