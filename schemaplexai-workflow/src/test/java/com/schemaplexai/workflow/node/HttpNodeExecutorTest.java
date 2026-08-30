package com.schemaplexai.workflow.node;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HttpNodeExecutorTest {

    private final HttpNodeExecutor executor = new HttpNodeExecutor();
    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void getNodeType_returnsHttp() {
        assertThat(executor.getNodeType()).isEqualTo("HTTP");
    }

    @Test
    void execute_nullUrl_returnsFailure() {
        NodeExecutionResult result = executor.execute(Map.of(), "tenant-1");
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.isRetryable()).isFalse();
        assertThat(result.getMessage()).contains("url");
        assertThat(result.getOutput()).isEmpty();
    }

    @Test
    void execute_blankUrl_returnsFailure() {
        NodeExecutionResult result = executor.execute(Map.of("url", "  "), "tenant-1");
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.isRetryable()).isFalse();
        assertThat(result.getMessage()).contains("url");
        assertThat(result.getOutput()).isEmpty();
    }

    @Test
    void execute_unsupportedMethod_returnsFailure() {
        NodeExecutionResult result = executor.execute(
                Map.of("url", "http://example.com", "method", "FOOBAR"), "tenant-1");
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.isRetryable()).isFalse();
    }

    @Test
    void execute_connectionRefused_retryableFailure() {
        NodeExecutionResult result = executor.execute(
                Map.of("url", "http://localhost:59999/test", "method", "GET"), "tenant-1");
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.isRetryable()).isTrue();
    }

    @Test
    void execute_perNodeTimeoutSeconds_honored() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/slow", exchange -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        int port = server.getAddress().getPort();

        long start = System.currentTimeMillis();
        NodeExecutionResult result = executor.execute(
                Map.of("url", "http://127.0.0.1:" + port + "/slow", "method", "GET", "timeoutSeconds", 1),
                "tenant-1");
        long elapsed = System.currentTimeMillis() - start;

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.isRetryable()).isTrue();
        assertThat(elapsed).isLessThan(2900);
    }

    @Test
    void execute_successfulGet_returnsStatusBodyHeaders() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/ok", exchange -> {
            byte[] body = "{\"k\":\"v\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        int port = server.getAddress().getPort();

        NodeExecutionResult result = executor.execute(
                Map.of("url", "http://127.0.0.1:" + port + "/ok", "method", "GET"), "tenant-1");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput()).containsEntry("statusCode", 200);
        assertThat(result.getOutput().get("body").toString()).contains("\"k\":\"v\"");
    }
}
