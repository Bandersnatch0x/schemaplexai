package com.schemaplexai.integration.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.integration.config.IntegrationConfig;
import com.schemaplexai.integration.dto.McpToolSchema;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Protocol-level tests for the real {@link McpClient} (issue 930), driven
 * against an in-process MCP stub server speaking the Streamable HTTP transport.
 */
@DisplayName("McpClient — real MCP protocol over HTTP")
class McpClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new IntegrationConfig()
            .restTemplate(Duration.ofSeconds(30), Duration.ofSeconds(30));

    private McpStubServer stub;

    @AfterEach
    void tearDown() {
        if (stub != null) {
            stub.close();
        }
    }

    private McpClient newClient() {
        return new McpClient(stub.endpoint(), restTemplate, objectMapper);
    }

    @Test
    @DisplayName("listTools performs initialize handshake then returns advertised tools")
    void listToolsHappyPath() throws IOException {
        stub = McpStubServer.start()
                .addTool("read_file", "Read a file", Map.of("type", "object",
                        "properties", Map.of("path", Map.of("type", "string"))))
                .addTool("write_file", "Write a file", null);

        List<McpToolSchema> tools = newClient().listTools();

        assertThat(tools).hasSize(2);
        assertThat(tools.get(0).getName()).isEqualTo("read_file");
        assertThat(tools.get(0).getDescription()).isEqualTo("Read a file");
        assertThat(tools.get(0).getInputSchema()).containsEntry("type", "object");
        assertThat(tools.get(1).getName()).isEqualTo("write_file");

        // Protocol order: initialize → notifications/initialized → tools/list
        assertThat(stub.requests).hasSize(3);
        assertThat(stub.requests.get(0).body()).contains("\"initialize\"");
        assertThat(stub.requests.get(1).body()).contains("notifications/initialized");
        assertThat(stub.requests.get(2).body()).contains("tools/list");
    }

    @Test
    @DisplayName("session id from initialize response is echoed on subsequent requests")
    void sessionIdPropagation() throws IOException {
        stub = McpStubServer.start().addTool("echo", "Echo tool", null);

        newClient().listTools();

        assertThat(stub.requests.get(0).sessionIdHeader()).isNull();
        assertThat(stub.requests.get(1).sessionIdHeader()).isEqualTo("stub-session-42");
        assertThat(stub.requests.get(2).sessionIdHeader()).isEqualTo("stub-session-42");
    }

    @Test
    @DisplayName("handshake succeeds even when the server does not issue a session id")
    void noSessionHeader() throws IOException {
        stub = McpStubServer.start().withoutSessionHeader().addTool("echo", "Echo tool", null);

        List<McpToolSchema> tools = newClient().listTools();

        assertThat(tools).hasSize(1);
        assertThat(stub.requests.get(2).sessionIdHeader()).isNull();
    }

    @Test
    @DisplayName("listTools follows nextCursor pagination to the last page")
    void listToolsPagination() throws IOException {
        stub = McpStubServer.start()
                .withPagination(true)
                .addTool("tool_a", "first page", null)
                .addSecondPageTool("tool_b", "second page");

        List<McpToolSchema> tools = newClient().listTools();

        assertThat(tools).extracting(McpToolSchema::getName)
                .containsExactly("tool_a", "tool_b");
        // The second tools/list call (requests[3]) must carry the cursor from page one
        assertThat(stub.requests).hasSize(4);
        assertThat(stub.requests.get(3).body()).contains("\"cursor\":\"page-2\"");
    }

    @Test
    @DisplayName("listTools on a reachable server exposing no tools returns an empty list")
    void listToolsEmptyIsLegitimate() throws IOException {
        stub = McpStubServer.start();

        List<McpToolSchema> tools = newClient().listTools();

        assertThat(tools).isEmpty();
    }

    @Test
    @DisplayName("initialize is performed only once across repeated calls")
    void initializedOnce() throws IOException {
        stub = McpStubServer.start().addTool("echo", "Echo tool", null);
        McpClient client = newClient();

        client.listTools();
        client.listTools();

        assertThat(client.isInitialized()).isTrue();
        long initializeCount = stub.requests.stream()
                .filter(r -> r.body().contains("\"initialize\""))
                .count();
        assertThat(initializeCount).isEqualTo(1);
        assertThat(stub.requests).hasSize(4); // init + notification + 2x tools/list
    }

    @Test
    @DisplayName("callTool sends tools/call and returns the JSON-RPC result")
    void callToolHappyPath() throws IOException {
        stub = McpStubServer.start().withCallToolText("42");

        String result = newClient().callTool("calculator", Map.of("expression", "6*7"));

        assertThat(result).contains("42");
        assertThat(stub.lastCallToolName).isEqualTo("calculator");
        assertThat(stub.lastCallArguments.path("expression").asText()).isEqualTo("6*7");
    }

    @Test
    @DisplayName("unreachable endpoint raises structured UNREACHABLE failure, never silent empty")
    void unreachableEndpointThrowsStructured() throws IOException {
        int closedPort = findClosedPort();
        McpClient client = new McpClient("http://127.0.0.1:" + closedPort, restTemplate, objectMapper);

        assertThatThrownBy(client::listTools)
                .isInstanceOf(McpClientException.class)
                .satisfies(ex -> {
                    McpClientException mce = (McpClientException) ex;
                    assertThat(mce.getKind()).isEqualTo(McpClientException.FailureKind.UNREACHABLE);
                    assertThat(mce.getEndpoint()).contains(String.valueOf(closedPort));
                    assertThat(mce.getMessage()).contains("unreachable");
                });
    }

    @Test
    @DisplayName("read-budget expiry raises structured TIMEOUT failure")
    void timeoutRaisesStructured() throws IOException {
        stub = McpStubServer.start().withDelayMillis(2000);
        RestTemplate fastTemplate = new IntegrationConfig()
                .restTemplate(Duration.ofSeconds(5), Duration.ofMillis(300));
        McpClient client = new McpClient(stub.endpoint(), fastTemplate, objectMapper);

        assertThatThrownBy(client::listTools)
                .isInstanceOf(McpClientException.class)
                .satisfies(ex -> {
                    McpClientException mce = (McpClientException) ex;
                    assertThat(mce.getKind()).isEqualTo(McpClientException.FailureKind.TIMEOUT);
                });
    }

    @Test
    @DisplayName("non-2xx answer raises structured HTTP_ERROR failure")
    void httpErrorRaisesStructured() throws IOException {
        stub = McpStubServer.start().withForcedStatus(503);

        assertThatThrownBy(() -> newClient().listTools())
                .isInstanceOf(McpClientException.class)
                .satisfies(ex -> {
                    McpClientException mce = (McpClientException) ex;
                    assertThat(mce.getKind()).isEqualTo(McpClientException.FailureKind.HTTP_ERROR);
                    assertThat(mce.getMessage()).contains("503");
                });
    }

    @Test
    @DisplayName("unparseable body raises structured PROTOCOL_ERROR failure")
    void garbageBodyRaisesProtocolError() throws IOException {
        stub = McpStubServer.start().withGarbageBody(true);

        assertThatThrownBy(() -> newClient().listTools())
                .isInstanceOf(McpClientException.class)
                .satisfies(ex -> assertThat(((McpClientException) ex).getKind())
                        .isEqualTo(McpClientException.FailureKind.PROTOCOL_ERROR));
    }

    @Test
    @DisplayName("JSON-RPC error object on tools/list raises structured PROTOCOL_ERROR")
    void jsonRpcErrorRaisesProtocolError() throws IOException {
        stub = McpStubServer.start().withToolsListError("listing unavailable");

        assertThatThrownBy(() -> newClient().listTools())
                .isInstanceOf(McpClientException.class)
                .satisfies(ex -> {
                    McpClientException mce = (McpClientException) ex;
                    assertThat(mce.getKind()).isEqualTo(McpClientException.FailureKind.PROTOCOL_ERROR);
                    assertThat(mce.getMessage()).contains("listing unavailable");
                });
    }

    @Test
    @DisplayName("callTool JSON-RPC error surfaces as structured failure")
    void callToolJsonRpcError() throws IOException {
        stub = McpStubServer.start();
        McpClient client = newClient();
        // Prime the handshake, then flip the stub into error mode for tools/call
        client.listTools();
        stub.withCallToolError("tool not found on server");

        assertThatThrownBy(() -> client.callTool("missing_tool", Map.of()))
                .isInstanceOf(McpClientException.class)
                .satisfies(ex -> {
                    McpClientException mce = (McpClientException) ex;
                    assertThat(mce.getKind()).isEqualTo(McpClientException.FailureKind.PROTOCOL_ERROR);
                    assertThat(mce.getMessage()).contains("tool not found on server");
                });
    }

    private static int findClosedPort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
