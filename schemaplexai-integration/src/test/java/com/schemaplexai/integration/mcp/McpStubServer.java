package com.schemaplexai.integration.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Lightweight in-process MCP server stub backed by the JDK HttpServer.
 * <p>
 * Implements the Streamable HTTP transport: {@code initialize} handshake
 * (echoing an {@code Mcp-Session-Id} header), {@code notifications/initialized},
 * {@code tools/list} (with optional two-page cursor pagination and JSON-RPC
 * error injection) and {@code tools/call}. Fault modes (non-2xx status,
 * garbage body, response delay) can be toggled per test. Issue 930.
 */
public final class McpStubServer implements AutoCloseable {

    /** One request as received by the stub. */
    public record RecordedRequest(String body, String sessionIdHeader) {
        public JsonNode json(ObjectMapper mapper) throws IOException {
            return mapper.readTree(body);
        }
    }

    private final HttpServer httpServer;
    private final ObjectMapper mapper = new ObjectMapper();

    /** Every request body received, in order. */
    public final List<RecordedRequest> requests = new CopyOnWriteArrayList<>();

    private final List<Map<String, Object>> firstPageTools = new ArrayList<>();
    private final List<Map<String, Object>> secondPageTools = new ArrayList<>();

    private volatile String sessionHeaderValue = "stub-session-42";
    private volatile boolean paginate;
    private volatile int forcedStatus = -1;
    private volatile boolean garbageBody;
    private volatile long delayMillis;
    private volatile String toolsListErrorMessage;
    private volatile String callToolErrorMessage;
    private volatile String callToolText = "stub-tool-output";

    /** Last arguments received by tools/call. */
    public volatile JsonNode lastCallArguments;
    /** Last tool name received by tools/call. */
    public volatile String lastCallToolName;

    private McpStubServer(HttpServer httpServer) {
        this.httpServer = httpServer;
        httpServer.createContext("/", this::handle);
    }

    public static McpStubServer start() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        McpStubServer stub = new McpStubServer(server);
        server.start();
        return stub;
    }

    public String endpoint() {
        return "http://127.0.0.1:" + httpServer.getAddress().getPort();
    }

    // ── configuration knobs ───────────────────────────────────────────

    public McpStubServer addTool(String name, String description, Map<String, Object> inputSchema) {
        firstPageTools.add(tool(name, description, inputSchema));
        return this;
    }

    public McpStubServer addSecondPageTool(String name, String description) {
        secondPageTools.add(tool(name, description, Map.of("type", "object")));
        return this;
    }

    public McpStubServer withPagination(boolean paginate) {
        this.paginate = paginate;
        return this;
    }

    public McpStubServer withForcedStatus(int status) {
        this.forcedStatus = status;
        return this;
    }

    public McpStubServer withGarbageBody(boolean garbage) {
        this.garbageBody = garbage;
        return this;
    }

    public McpStubServer withDelayMillis(long delayMillis) {
        this.delayMillis = delayMillis;
        return this;
    }

    public McpStubServer withToolsListError(String message) {
        this.toolsListErrorMessage = message;
        return this;
    }

    public McpStubServer withCallToolError(String message) {
        this.callToolErrorMessage = message;
        return this;
    }

    public McpStubServer withCallToolText(String text) {
        this.callToolText = text;
        return this;
    }

    public McpStubServer withoutSessionHeader() {
        this.sessionHeaderValue = null;
        return this;
    }

    private static Map<String, Object> tool(String name, String description, Map<String, Object> schema) {
        Map<String, Object> tool = new LinkedHashMap<>();
        tool.put("name", name);
        tool.put("description", description);
        tool.put("inputSchema", schema != null ? schema : Map.of("type", "object"));
        return tool;
    }

    // ── request handling ──────────────────────────────────────────────

    private void handle(HttpExchange exchange) throws IOException {
        String body;
        try (InputStream in = exchange.getRequestBody()) {
            body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        String sessionHeader = exchange.getRequestHeaders().getFirst("Mcp-Session-Id");
        requests.add(new RecordedRequest(body, sessionHeader));

        if (delayMillis > 0) {
            try {
                Thread.sleep(delayMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        if (forcedStatus > 0) {
            respond(exchange, forcedStatus, "{\"error\":\"forced\"}");
            return;
        }
        if (garbageBody) {
            respond(exchange, 200, "this is definitely not json-rpc");
            return;
        }

        JsonNode request;
        try {
            request = mapper.readTree(body);
        } catch (Exception e) {
            respond(exchange, 400, "{\"error\":\"unparseable\"}");
            return;
        }

        String method = request.path("method").asText("");
        Object id = request.has("id") ? request.get("id").numberValue() : null;

        switch (method) {
            case "initialize" -> {
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("protocolVersion", "2024-11-05");
                result.put("capabilities", Map.of("tools", Map.of("listChanged", true)));
                result.put("serverInfo", Map.of("name", "mcp-stub", "version", "1.0.0"));
                if (sessionHeaderValue != null) {
                    exchange.getResponseHeaders().set("Mcp-Session-Id", sessionHeaderValue);
                }
                respondJsonRpc(exchange, id, result, null);
            }
            case "notifications/initialized" -> respond(exchange, 202, null);
            case "tools/list" -> {
                if (toolsListErrorMessage != null) {
                    respondJsonRpc(exchange, id, null, toolsListErrorMessage);
                    return;
                }
                String cursor = request.path("params").path("cursor").asText(null);
                Map<String, Object> result = new LinkedHashMap<>();
                if (paginate && cursor == null) {
                    result.put("tools", firstPageTools);
                    result.put("nextCursor", "page-2");
                } else if (paginate) {
                    result.put("tools", secondPageTools);
                } else {
                    result.put("tools", firstPageTools);
                }
                respondJsonRpc(exchange, id, result, null);
            }
            case "tools/call" -> {
                if (callToolErrorMessage != null) {
                    respondJsonRpc(exchange, id, null, callToolErrorMessage);
                    return;
                }
                JsonNode params = request.path("params");
                lastCallToolName = params.path("name").asText(null);
                lastCallArguments = params.path("arguments");
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("content", List.of(Map.of("type", "text", "text", callToolText)));
                result.put("isError", false);
                respondJsonRpc(exchange, id, result, null);
            }
            default -> respondJsonRpc(exchange, id, null, "method not found: " + method);
        }
    }

    private void respondJsonRpc(HttpExchange exchange, Object id,
                                Map<String, Object> result, String errorMessage) throws IOException {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", "2.0");
        if (errorMessage != null) {
            response.put("error", Map.of("code", -32601, "message", errorMessage));
        } else {
            response.put("result", result);
        }
        response.put("id", id);
        respond(exchange, 200, mapper.writeValueAsString(response));
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        if (body == null) {
            exchange.sendResponseHeaders(status, -1);
        } else {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        }
        exchange.close();
    }

    @Override
    public void close() {
        httpServer.stop(0);
    }
}
