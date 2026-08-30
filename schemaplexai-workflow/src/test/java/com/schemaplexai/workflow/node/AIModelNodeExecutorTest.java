package com.schemaplexai.workflow.node;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AIModelNodeExecutorTest {

    private final AIModelNodeExecutor executor = new AIModelNodeExecutor();
    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    private int startServer(com.sun.net.httpserver.HttpHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/agent/execute", handler);
        server.start();
        return server.getAddress().getPort();
    }

    @Test
    void execute_slowAgentEngine_returnsTimeoutResult() throws IOException {
        int port = startServer(exchange -> {
            try {
                // Hold the connection past the node's timeout budget.
                Thread.sleep(3000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        ReflectionTestUtils.setField(executor, "agentEngineUrl", "http://127.0.0.1:" + port);

        NodeExecutionResult result = executor.execute(
                Map.of("prompt", "hello", "timeoutSeconds", 1), "tenant-1");

        assertThat(result.isTimeout()).isTrue();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("timed out after 1s");
    }

    @Test
    void execute_agentEngineReturnsData_success() throws IOException {
        int port = startServer(exchange -> {
            exchange.getRequestBody().readAllBytes();
            byte[] body = "{\"code\":200,\"data\":\"generated answer\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        ReflectionTestUtils.setField(executor, "agentEngineUrl", "http://127.0.0.1:" + port);

        NodeExecutionResult result = executor.execute(
                Map.of("prompt", "hello", "agentId", "42", "modelId", "m-1"), "tenant-1");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput()).containsEntry("generatedText", "generated answer");
        assertThat(result.getOutput()).containsEntry("agentId", "42");
        assertThat(result.getOutput()).containsEntry("modelUsed", "m-1");
    }

    @Test
    void execute_connectionRefused_retryableFailure() {
        ReflectionTestUtils.setField(executor, "agentEngineUrl", "http://127.0.0.1:59998");

        NodeExecutionResult result = executor.execute(Map.of("prompt", "hello"), "tenant-1");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.isRetryable()).isTrue();
        assertThat(result.isTimeout()).isFalse();
    }

    @Test
    void execute_missingPrompt_deterministicFailure() {
        NodeExecutionResult result = executor.execute(Map.of(), "tenant-1");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.isRetryable()).isFalse();
        assertThat(result.getMessage()).contains("prompt");
    }

    @Test
    void defaultTimeout_is300Seconds() {
        assertThat(AIModelNodeExecutor.DEFAULT_TIMEOUT_SECONDS).isEqualTo(300);
    }
}
