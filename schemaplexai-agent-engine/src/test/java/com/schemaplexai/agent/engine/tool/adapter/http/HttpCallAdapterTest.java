package com.schemaplexai.agent.engine.tool.adapter.http;

import com.schemaplexai.agent.engine.config.SecurityPolicyLoader;
import com.schemaplexai.agent.engine.model.LlmProvider;
import com.schemaplexai.agent.engine.tool.ToolCall;
import com.schemaplexai.agent.engine.tool.ToolErrorCategory;
import com.schemaplexai.agent.engine.tool.ToolExecutionException;
import com.schemaplexai.agent.engine.tool.ToolResult;
import com.schemaplexai.agent.engine.tool.adapter.ExecutionContext;
import com.schemaplexai.model.entity.config.TenantEnvironmentConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.InetAddress;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HttpCallAdapterTest {

    @Mock
    private SecurityPolicyLoader securityPolicyLoader;

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

    private HttpCallAdapter adapter;

    private static MockedStatic<InetAddress> inetAddressMock;
    private static InetAddress publicIp;

    @BeforeAll
    static void mockDnsResolution() throws Exception {
        // Create the return value BEFORE opening the static mock
        publicIp = InetAddress.getByAddress(new byte[]{1, 1, 1, 1});
        inetAddressMock = mockStatic(InetAddress.class, CALLS_REAL_METHODS);
        // Stub DNS for example.com so tests work offline / in CI
        inetAddressMock.when(() -> InetAddress.getByName("example.com"))
                .thenReturn(publicIp);
    }

    @AfterAll
    static void closeDnsMock() {
        if (inetAddressMock != null) {
            inetAddressMock.close();
        }
    }

    @BeforeEach
    void setUp() {
        adapter = new HttpCallAdapter(securityPolicyLoader);
    }

    @Test
    void shouldReturnToolName() {
        assertEquals("http_call", adapter.getToolName());
    }

    @Test
    void shouldRejectBlankUrl() {
        ToolCall call = new ToolCall("http_call", Map.of("url", ""));
        ExecutionContext ctx = new ExecutionContext("tenant1", 1L, "/workspace");

        ToolExecutionException ex = assertThrows(ToolExecutionException.class,
                () -> adapter.execute(call, ctx));

        assertEquals(ToolErrorCategory.INVALID_ARGUMENT, ex.getErrorCategory());
        assertTrue(ex.getMessage().contains("URL is required"));
    }

    @Test
    void shouldRejectDisallowedHttpMethod() {
        ToolCall call = new ToolCall("http_call", Map.of("url", "http://example.com", "method", "TRACE"));
        ExecutionContext ctx = new ExecutionContext("tenant1", 1L, "/workspace");

        ToolExecutionException ex = assertThrows(ToolExecutionException.class,
                () -> adapter.execute(call, ctx));

        assertEquals(ToolErrorCategory.INVALID_ARGUMENT, ex.getErrorCategory());
        assertTrue(ex.getMessage().contains("HTTP method not allowed"));
    }

    @Test
    void shouldRejectConnectMethod() {
        ToolCall call = new ToolCall("http_call", Map.of("url", "http://example.com", "method", "CONNECT"));
        ExecutionContext ctx = new ExecutionContext("tenant1", 1L, "/workspace");

        ToolExecutionException ex = assertThrows(ToolExecutionException.class,
                () -> adapter.execute(call, ctx));

        assertEquals(ToolErrorCategory.INVALID_ARGUMENT, ex.getErrorCategory());
    }

    @Test
    void shouldBlockWhenTenantDisallowsHttpCalls() {
        TenantEnvironmentConfig config = new TenantEnvironmentConfig();
        config.setAllowHttpCalls(false);
        when(securityPolicyLoader.load("tenant1")).thenReturn(config);

        ToolCall call = new ToolCall("http_call", Map.of("url", "http://example.com"));
        ExecutionContext ctx = new ExecutionContext("tenant1", 1L, "/workspace");

        ToolExecutionException ex = assertThrows(ToolExecutionException.class,
                () -> adapter.execute(call, ctx));

        assertEquals(ToolErrorCategory.ENVIRONMENT_MISMATCH, ex.getErrorCategory());
        assertTrue(ex.getMessage().contains("HTTP calls are not enabled"));
    }

    @Test
    void shouldBlockWhenTenantConfigAllowHttpCallsIsNull() {
        TenantEnvironmentConfig config = new TenantEnvironmentConfig();
        config.setAllowHttpCalls(null);
        when(securityPolicyLoader.load("tenant1")).thenReturn(config);

        ToolCall call = new ToolCall("http_call", Map.of("url", "http://example.com"));
        ExecutionContext ctx = new ExecutionContext("tenant1", 1L, "/workspace");

        ToolExecutionException ex = assertThrows(ToolExecutionException.class,
                () -> adapter.execute(call, ctx));

        assertEquals(ToolErrorCategory.ENVIRONMENT_MISMATCH, ex.getErrorCategory());
    }

    @Test
    void shouldAllowWhenTenantPermitsHttpCalls() throws Exception {
        TenantEnvironmentConfig config = new TenantEnvironmentConfig();
        config.setAllowHttpCalls(true);
        when(securityPolicyLoader.load("tenant1")).thenReturn(config);

        // Use reflection to inject mock HttpClient
        java.lang.reflect.Field field = HttpCallAdapter.class.getDeclaredField("httpClient");
        field.setAccessible(true);
        field.set(adapter, httpClient);

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("OK");

        ToolCall call = new ToolCall("http_call", Map.of("url", "http://example.com"));
        ExecutionContext ctx = new ExecutionContext("tenant1", 1L, "/workspace");

        ToolResult result = adapter.execute(call, ctx);

        assertTrue(result.success());
        assertEquals("OK", result.output());
    }

    @Test
    void shouldBlockPrivateIp() {
        TenantEnvironmentConfig config = new TenantEnvironmentConfig();
        config.setAllowHttpCalls(true);
        when(securityPolicyLoader.load("tenant1")).thenReturn(config);

        ToolCall call = new ToolCall("http_call", Map.of("url", "http://192.168.1.1"));
        ExecutionContext ctx = new ExecutionContext("tenant1", 1L, "/workspace");

        ToolExecutionException ex = assertThrows(ToolExecutionException.class,
                () -> adapter.execute(call, ctx));

        assertEquals(ToolErrorCategory.ENVIRONMENT_MISMATCH, ex.getErrorCategory());
        assertTrue(ex.getMessage().contains("private/internal IP addresses not allowed"));
    }

    @Test
    void shouldBlockFileScheme() {
        TenantEnvironmentConfig config = new TenantEnvironmentConfig();
        config.setAllowHttpCalls(true);
        when(securityPolicyLoader.load("tenant1")).thenReturn(config);

        ToolCall call = new ToolCall("http_call", Map.of("url", "file:///etc/passwd"));
        ExecutionContext ctx = new ExecutionContext("tenant1", 1L, "/workspace");

        ToolExecutionException ex = assertThrows(ToolExecutionException.class,
                () -> adapter.execute(call, ctx));

        assertEquals(ToolErrorCategory.ENVIRONMENT_MISMATCH, ex.getErrorCategory());
        assertTrue(ex.getMessage().contains("Blocked URL scheme"));
    }

    @Test
    void shouldBlockFtpScheme() {
        TenantEnvironmentConfig config = new TenantEnvironmentConfig();
        config.setAllowHttpCalls(true);
        when(securityPolicyLoader.load("tenant1")).thenReturn(config);

        ToolCall call = new ToolCall("http_call", Map.of("url", "ftp://example.com"));
        ExecutionContext ctx = new ExecutionContext("tenant1", 1L, "/workspace");

        ToolExecutionException ex = assertThrows(ToolExecutionException.class,
                () -> adapter.execute(call, ctx));

        assertEquals(ToolErrorCategory.ENVIRONMENT_MISMATCH, ex.getErrorCategory());
    }

    @Test
    void shouldBlockGopherScheme() {
        TenantEnvironmentConfig config = new TenantEnvironmentConfig();
        config.setAllowHttpCalls(true);
        when(securityPolicyLoader.load("tenant1")).thenReturn(config);

        ToolCall call = new ToolCall("http_call", Map.of("url", "gopher://example.com"));
        ExecutionContext ctx = new ExecutionContext("tenant1", 1L, "/workspace");

        ToolExecutionException ex = assertThrows(ToolExecutionException.class,
                () -> adapter.execute(call, ctx));

        assertEquals(ToolErrorCategory.ENVIRONMENT_MISMATCH, ex.getErrorCategory());
    }

    @Test
    void shouldBlockJarScheme() {
        TenantEnvironmentConfig config = new TenantEnvironmentConfig();
        config.setAllowHttpCalls(true);
        when(securityPolicyLoader.load("tenant1")).thenReturn(config);

        ToolCall call = new ToolCall("http_call", Map.of("url", "jar:file:///test.jar"));
        ExecutionContext ctx = new ExecutionContext("tenant1", 1L, "/workspace");

        ToolExecutionException ex = assertThrows(ToolExecutionException.class,
                () -> adapter.execute(call, ctx));

        assertEquals(ToolErrorCategory.ENVIRONMENT_MISMATCH, ex.getErrorCategory());
    }

    @Test
    void shouldBlockUnknownScheme() {
        TenantEnvironmentConfig config = new TenantEnvironmentConfig();
        config.setAllowHttpCalls(true);
        when(securityPolicyLoader.load("tenant1")).thenReturn(config);

        ToolCall call = new ToolCall("http_call", Map.of("url", "unknown://example.com"));
        ExecutionContext ctx = new ExecutionContext("tenant1", 1L, "/workspace");

        ToolExecutionException ex = assertThrows(ToolExecutionException.class,
                () -> adapter.execute(call, ctx));

        assertEquals(ToolErrorCategory.ENVIRONMENT_MISMATCH, ex.getErrorCategory());
        assertTrue(ex.getMessage().contains("Only HTTP/HTTPS schemes allowed"));
    }

    @Test
    void shouldBlockNullScheme() {
        TenantEnvironmentConfig config = new TenantEnvironmentConfig();
        config.setAllowHttpCalls(true);
        when(securityPolicyLoader.load("tenant1")).thenReturn(config);

        ToolCall call = new ToolCall("http_call", Map.of("url", "example.com"));
        ExecutionContext ctx = new ExecutionContext("tenant1", 1L, "/workspace");

        ToolExecutionException ex = assertThrows(ToolExecutionException.class,
                () -> adapter.execute(call, ctx));

        assertEquals(ToolErrorCategory.ENVIRONMENT_MISMATCH, ex.getErrorCategory());
    }

    @Test
    void shouldBlockInvalidUrl() {
        TenantEnvironmentConfig config = new TenantEnvironmentConfig();
        config.setAllowHttpCalls(true);
        when(securityPolicyLoader.load("tenant1")).thenReturn(config);

        ToolCall call = new ToolCall("http_call", Map.of("url", "http://[invalid"));
        ExecutionContext ctx = new ExecutionContext("tenant1", 1L, "/workspace");

        ToolExecutionException ex = assertThrows(ToolExecutionException.class,
                () -> adapter.execute(call, ctx));

        assertEquals(ToolErrorCategory.INVALID_ARGUMENT, ex.getErrorCategory());
        assertTrue(ex.getMessage().contains("Invalid URL"));
    }

    @Test
    void shouldBlockUrlWithoutHost() {
        TenantEnvironmentConfig config = new TenantEnvironmentConfig();
        config.setAllowHttpCalls(true);
        when(securityPolicyLoader.load("tenant1")).thenReturn(config);

        ToolCall call = new ToolCall("http_call", Map.of("url", "http:///path"));
        ExecutionContext ctx = new ExecutionContext("tenant1", 1L, "/workspace");

        ToolExecutionException ex = assertThrows(ToolExecutionException.class,
                () -> adapter.execute(call, ctx));

        assertEquals(ToolErrorCategory.INVALID_ARGUMENT, ex.getErrorCategory());
        assertTrue(ex.getMessage().contains("URL has no host"));
    }

    @Test
    void shouldAllowWhenContextIsNull() throws Exception {
        // Use reflection to inject mock HttpClient
        java.lang.reflect.Field field = HttpCallAdapter.class.getDeclaredField("httpClient");
        field.setAccessible(true);
        field.set(adapter, httpClient);

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("OK");

        ToolCall call = new ToolCall("http_call", Map.of("url", "http://example.com"));

        ToolResult result = adapter.execute(call, null);

        assertTrue(result.success());
    }

    @Test
    void shouldAllowWhenTenantIdIsNull() throws Exception {
        // Use reflection to inject mock HttpClient
        java.lang.reflect.Field field = HttpCallAdapter.class.getDeclaredField("httpClient");
        field.setAccessible(true);
        field.set(adapter, httpClient);

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("OK");

        ToolCall call = new ToolCall("http_call", Map.of("url", "http://example.com"));
        ExecutionContext ctx = new ExecutionContext(null, 1L, "/workspace");

        ToolResult result = adapter.execute(call, ctx);

        assertTrue(result.success());
    }

    @Test
    void shouldHandlePostWithBody() throws Exception {
        TenantEnvironmentConfig config = new TenantEnvironmentConfig();
        config.setAllowHttpCalls(true);
        when(securityPolicyLoader.load("tenant1")).thenReturn(config);

        java.lang.reflect.Field field = HttpCallAdapter.class.getDeclaredField("httpClient");
        field.setAccessible(true);
        field.set(adapter, httpClient);

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(201);
        when(httpResponse.body()).thenReturn("Created");

        ToolCall call = new ToolCall("http_call", Map.of(
                "url", "http://example.com",
                "method", "POST",
                "body", "{\"key\":\"value\"}"
        ));
        ExecutionContext ctx = new ExecutionContext("tenant1", 1L, "/workspace");

        ToolResult result = adapter.execute(call, ctx);

        assertTrue(result.success());
        assertEquals("Created", result.output());
    }

    @Test
    void shouldHandlePutWithoutBody() throws Exception {
        TenantEnvironmentConfig config = new TenantEnvironmentConfig();
        config.setAllowHttpCalls(true);
        when(securityPolicyLoader.load("tenant1")).thenReturn(config);

        java.lang.reflect.Field field = HttpCallAdapter.class.getDeclaredField("httpClient");
        field.setAccessible(true);
        field.set(adapter, httpClient);

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("Updated");

        ToolCall call = new ToolCall("http_call", Map.of(
                "url", "http://example.com",
                "method", "PUT"
        ));
        ExecutionContext ctx = new ExecutionContext("tenant1", 1L, "/workspace");

        ToolResult result = adapter.execute(call, ctx);

        assertTrue(result.success());
    }

    @Test
    void shouldHandleDeleteMethod() throws Exception {
        TenantEnvironmentConfig config = new TenantEnvironmentConfig();
        config.setAllowHttpCalls(true);
        when(securityPolicyLoader.load("tenant1")).thenReturn(config);

        java.lang.reflect.Field field = HttpCallAdapter.class.getDeclaredField("httpClient");
        field.setAccessible(true);
        field.set(adapter, httpClient);

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(204);
        when(httpResponse.body()).thenReturn("");

        ToolCall call = new ToolCall("http_call", Map.of(
                "url", "http://example.com",
                "method", "DELETE"
        ));
        ExecutionContext ctx = new ExecutionContext("tenant1", 1L, "/workspace");

        ToolResult result = adapter.execute(call, ctx);

        assertTrue(result.success());
    }

    @Test
    void shouldHandlePatchMethod() throws Exception {
        TenantEnvironmentConfig config = new TenantEnvironmentConfig();
        config.setAllowHttpCalls(true);
        when(securityPolicyLoader.load("tenant1")).thenReturn(config);

        java.lang.reflect.Field field = HttpCallAdapter.class.getDeclaredField("httpClient");
        field.setAccessible(true);
        field.set(adapter, httpClient);

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("Patched");

        ToolCall call = new ToolCall("http_call", Map.of(
                "url", "http://example.com",
                "method", "PATCH",
                "body", "patch data"
        ));
        ExecutionContext ctx = new ExecutionContext("tenant1", 1L, "/workspace");

        ToolResult result = adapter.execute(call, ctx);

        assertTrue(result.success());
    }

    @Test
    void shouldHandleHeadMethod() throws Exception {
        TenantEnvironmentConfig config = new TenantEnvironmentConfig();
        config.setAllowHttpCalls(true);
        when(securityPolicyLoader.load("tenant1")).thenReturn(config);

        java.lang.reflect.Field field = HttpCallAdapter.class.getDeclaredField("httpClient");
        field.setAccessible(true);
        field.set(adapter, httpClient);

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("");

        ToolCall call = new ToolCall("http_call", Map.of(
                "url", "http://example.com",
                "method", "HEAD"
        ));
        ExecutionContext ctx = new ExecutionContext("tenant1", 1L, "/workspace");

        ToolResult result = adapter.execute(call, ctx);

        assertTrue(result.success());
    }

    @Test
    void shouldHandleOptionsMethod() throws Exception {
        TenantEnvironmentConfig config = new TenantEnvironmentConfig();
        config.setAllowHttpCalls(true);
        when(securityPolicyLoader.load("tenant1")).thenReturn(config);

        java.lang.reflect.Field field = HttpCallAdapter.class.getDeclaredField("httpClient");
        field.setAccessible(true);
        field.set(adapter, httpClient);

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("Allow: GET, POST");

        ToolCall call = new ToolCall("http_call", Map.of(
                "url", "http://example.com",
                "method", "OPTIONS"
        ));
        ExecutionContext ctx = new ExecutionContext("tenant1", 1L, "/workspace");

        ToolResult result = adapter.execute(call, ctx);

        assertTrue(result.success());
    }

    @Test
    void shouldHandleNullBodyForPost() throws Exception {
        TenantEnvironmentConfig config = new TenantEnvironmentConfig();
        config.setAllowHttpCalls(true);
        when(securityPolicyLoader.load("tenant1")).thenReturn(config);

        java.lang.reflect.Field field = HttpCallAdapter.class.getDeclaredField("httpClient");
        field.setAccessible(true);
        field.set(adapter, httpClient);

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("OK");

        ToolCall call = new ToolCall("http_call", Map.of(
                "url", "http://example.com",
                "method", "POST"
        ));
        ExecutionContext ctx = new ExecutionContext("tenant1", 1L, "/workspace");

        ToolResult result = adapter.execute(call, ctx);

        assertTrue(result.success());
    }

    @Test
    void shouldHandleHttpClientException() throws Exception {
        TenantEnvironmentConfig config = new TenantEnvironmentConfig();
        config.setAllowHttpCalls(true);
        when(securityPolicyLoader.load("tenant1")).thenReturn(config);

        java.lang.reflect.Field field = HttpCallAdapter.class.getDeclaredField("httpClient");
        field.setAccessible(true);
        field.set(adapter, httpClient);

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new java.io.IOException("Connection refused"));

        ToolCall call = new ToolCall("http_call", Map.of("url", "http://example.com"));
        ExecutionContext ctx = new ExecutionContext("tenant1", 1L, "/workspace");

        ToolExecutionException ex = assertThrows(ToolExecutionException.class,
                () -> adapter.execute(call, ctx));

        assertEquals(ToolErrorCategory.INTERNAL_ERROR, ex.getErrorCategory());
        assertTrue(ex.getMessage().contains("HTTP request failed"));
    }

    @Test
    void shouldHandleRedirect() throws Exception {
        TenantEnvironmentConfig config = new TenantEnvironmentConfig();
        config.setAllowHttpCalls(true);
        when(securityPolicyLoader.load("tenant1")).thenReturn(config);

        java.lang.reflect.Field field = HttpCallAdapter.class.getDeclaredField("httpClient");
        field.setAccessible(true);
        field.set(adapter, httpClient);

        java.net.http.HttpResponse<String> redirectResponse = mock(java.net.http.HttpResponse.class);
        when(redirectResponse.statusCode()).thenReturn(302);
        when(redirectResponse.headers()).thenReturn(java.net.http.HttpHeaders.of(
                Map.of("Location", List.of("http://1.1.1.1/redirect")),
                (a, b) -> true));

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("Final");

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(redirectResponse)
                .thenReturn(httpResponse);

        ToolCall call = new ToolCall("http_call", Map.of("url", "http://example.com"));
        ExecutionContext ctx = new ExecutionContext("tenant1", 1L, "/workspace");

        ToolResult result = adapter.execute(call, ctx);

        assertTrue(result.success());
        assertEquals("Final", result.output());
    }

    @Test
    void shouldHandleRelativeRedirect() throws Exception {
        TenantEnvironmentConfig config = new TenantEnvironmentConfig();
        config.setAllowHttpCalls(true);
        when(securityPolicyLoader.load("tenant1")).thenReturn(config);

        java.lang.reflect.Field field = HttpCallAdapter.class.getDeclaredField("httpClient");
        field.setAccessible(true);
        field.set(adapter, httpClient);

        java.net.http.HttpResponse<String> redirectResponse = mock(java.net.http.HttpResponse.class);
        when(redirectResponse.statusCode()).thenReturn(301);
        when(redirectResponse.headers()).thenReturn(java.net.http.HttpHeaders.of(
                Map.of("Location", List.of("/new-path")),
                (a, b) -> true));

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("Final");

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(redirectResponse)
                .thenReturn(httpResponse);

        ToolCall call = new ToolCall("http_call", Map.of("url", "http://example.com/old"));
        ExecutionContext ctx = new ExecutionContext("tenant1", 1L, "/workspace");

        ToolResult result = adapter.execute(call, ctx);

        assertTrue(result.success());
    }

    @Test
    void shouldHandleTooManyRedirects() throws Exception {
        TenantEnvironmentConfig config = new TenantEnvironmentConfig();
        config.setAllowHttpCalls(true);
        when(securityPolicyLoader.load("tenant1")).thenReturn(config);

        java.lang.reflect.Field field = HttpCallAdapter.class.getDeclaredField("httpClient");
        field.setAccessible(true);
        field.set(adapter, httpClient);

        java.net.http.HttpResponse<String> redirectResponse = mock(java.net.http.HttpResponse.class);
        when(redirectResponse.statusCode()).thenReturn(302);
        when(redirectResponse.headers()).thenReturn(java.net.http.HttpHeaders.of(
                Map.of("Location", List.of("http://example.com/redirect")),
                (a, b) -> true));

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(redirectResponse);

        ToolCall call = new ToolCall("http_call", Map.of("url", "http://example.com"));
        ExecutionContext ctx = new ExecutionContext("tenant1", 1L, "/workspace");

        ToolExecutionException ex = assertThrows(ToolExecutionException.class,
                () -> adapter.execute(call, ctx));

        assertEquals(ToolErrorCategory.ENVIRONMENT_MISMATCH, ex.getErrorCategory());
        assertTrue(ex.getMessage().contains("Too many redirects"));
    }

    @Test
    void shouldHandleInvalidRedirectUrl() throws Exception {
        TenantEnvironmentConfig config = new TenantEnvironmentConfig();
        config.setAllowHttpCalls(true);
        when(securityPolicyLoader.load("tenant1")).thenReturn(config);

        java.lang.reflect.Field field = HttpCallAdapter.class.getDeclaredField("httpClient");
        field.setAccessible(true);
        field.set(adapter, httpClient);

        java.net.http.HttpResponse<String> redirectResponse = mock(java.net.http.HttpResponse.class);
        when(redirectResponse.statusCode()).thenReturn(302);
        when(redirectResponse.headers()).thenReturn(java.net.http.HttpHeaders.of(
                Map.of("Location", List.of("http://[invalid")),
                (a, b) -> true));

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(redirectResponse);

        ToolCall call = new ToolCall("http_call", Map.of("url", "http://example.com"));
        ExecutionContext ctx = new ExecutionContext("tenant1", 1L, "/workspace");

        ToolExecutionException ex = assertThrows(ToolExecutionException.class,
                () -> adapter.execute(call, ctx));

        assertEquals(ToolErrorCategory.INVALID_ARGUMENT, ex.getErrorCategory());
        assertTrue(ex.getMessage().contains("Invalid redirect URL"));
    }

    @Test
    void shouldHandleRedirectWithoutLocation() throws Exception {
        TenantEnvironmentConfig config = new TenantEnvironmentConfig();
        config.setAllowHttpCalls(true);
        when(securityPolicyLoader.load("tenant1")).thenReturn(config);

        java.lang.reflect.Field field = HttpCallAdapter.class.getDeclaredField("httpClient");
        field.setAccessible(true);
        field.set(adapter, httpClient);

        java.net.http.HttpResponse<String> redirectResponse = mock(java.net.http.HttpResponse.class);
        when(redirectResponse.statusCode()).thenReturn(302);
        when(redirectResponse.headers()).thenReturn(java.net.http.HttpHeaders.of(
                Map.of(), (a, b) -> true));
        when(redirectResponse.body()).thenReturn("Redirect without location");

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(redirectResponse);

        ToolCall call = new ToolCall("http_call", Map.of("url", "http://example.com"));
        ExecutionContext ctx = new ExecutionContext("tenant1", 1L, "/workspace");

        ToolResult result = adapter.execute(call, ctx);

        assertTrue(result.success());
        assertEquals("Redirect without location", result.output());
    }

    @Test
    void shouldHandleNullResponseBody() throws Exception {
        TenantEnvironmentConfig config = new TenantEnvironmentConfig();
        config.setAllowHttpCalls(true);
        when(securityPolicyLoader.load("tenant1")).thenReturn(config);

        java.lang.reflect.Field field = HttpCallAdapter.class.getDeclaredField("httpClient");
        field.setAccessible(true);
        field.set(adapter, httpClient);

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(null);

        ToolCall call = new ToolCall("http_call", Map.of("url", "http://example.com"));
        ExecutionContext ctx = new ExecutionContext("tenant1", 1L, "/workspace");

        ToolResult result = adapter.execute(call, ctx);

        assertTrue(result.success());
        assertEquals("", result.output());
    }

    @Test
    void shouldHandleDnsResolutionFailure() {
        TenantEnvironmentConfig config = new TenantEnvironmentConfig();
        config.setAllowHttpCalls(true);
        when(securityPolicyLoader.load("tenant1")).thenReturn(config);

        ToolCall call = new ToolCall("http_call", Map.of("url", "http://this-host-definitely-does-not-exist-12345.local"));
        ExecutionContext ctx = new ExecutionContext("tenant1", 1L, "/workspace");

        ToolExecutionException ex = assertThrows(ToolExecutionException.class,
                () -> adapter.execute(call, ctx));

        assertEquals(ToolErrorCategory.INTERNAL_ERROR, ex.getErrorCategory());
        assertTrue(ex.getMessage().contains("DNS resolution failed") || ex.getMessage().contains("HTTP call failed"));
    }
}
