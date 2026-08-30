package com.schemaplexai.task.service;

import com.schemaplexai.common.exception.BaseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Tests the context-service client against a mocked HTTP server (MockRestServiceServer).
 * No real schemaplexai-context instance is required.
 */
class ContextServiceClientTest {

    private static final String BASE_URL = "http://context.test";

    private MockRestServiceServer mockServer;
    private ContextServiceClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        mockServer = MockRestServiceServer.bindTo(builder).build();
        client = new ContextServiceClient(builder.build());
    }

    // ------------------------------------------------------------------
    // syncDocument
    // ------------------------------------------------------------------

    @Test
    void syncDocument_success_callsInternalEndpointWithTenantHeader() {
        mockServer.expect(requestTo(BASE_URL + "/context/internal/milvus-sync/11"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Tenant-Id", "tenant-a"))
                .andRespond(withSuccess("{\"code\":200,\"message\":\"success\",\"data\":true}",
                        MediaType.APPLICATION_JSON));

        client.syncDocument(11L, "tenant-a");

        mockServer.verify();
    }

    @Test
    void syncDocument_businessErrorEnvelope_throwsBaseException() {
        mockServer.expect(requestTo(BASE_URL + "/context/internal/milvus-sync/11"))
                .andRespond(withSuccess("{\"code\":8002,\"message\":\"knowledge document not found\"}",
                        MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.syncDocument(11L, "tenant-a"))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("knowledge document not found");

        mockServer.verify();
    }

    @Test
    void syncDocument_httpError_throwsBaseException() {
        mockServer.expect(requestTo(BASE_URL + "/context/internal/milvus-sync/11"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.syncDocument(11L, "tenant-a"))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("HTTP 500");

        mockServer.verify();
    }

    @Test
    void syncDocument_emptyBody_throwsBaseException() {
        mockServer.expect(requestTo(BASE_URL + "/context/internal/milvus-sync/11"))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.syncDocument(11L, "tenant-a"))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("empty response");

        mockServer.verify();
    }

    // ------------------------------------------------------------------
    // countVectors
    // ------------------------------------------------------------------

    @Test
    void countVectors_success_returnsDataValue() {
        mockServer.expect(requestTo(BASE_URL + "/context/internal/milvus-sync/docs/11/vector-count"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Tenant-Id", "tenant-a"))
                .andRespond(withSuccess("{\"code\":200,\"message\":\"success\",\"data\":7}",
                        MediaType.APPLICATION_JSON));

        assertThat(client.countVectors(11L, "tenant-a")).isEqualTo(7L);

        mockServer.verify();
    }

    @Test
    void countVectors_milvusDisabledEnvelope_throwsBaseException() {
        // Context service answers HTTP 200 with a 503-code Result envelope when Milvus is off.
        mockServer.expect(requestTo(BASE_URL + "/context/internal/milvus-sync/docs/11/vector-count"))
                .andRespond(withSuccess("{\"code\":503,\"message\":\"Milvus is disabled; vector count unavailable\"}",
                        MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.countVectors(11L, "tenant-a"))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("Milvus is disabled");

        mockServer.verify();
    }

    @Test
    void countVectors_httpError_throwsBaseException() {
        mockServer.expect(requestTo(BASE_URL + "/context/internal/milvus-sync/docs/11/vector-count"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.countVectors(11L, "tenant-a"))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining("HTTP 500");

        mockServer.verify();
    }
}
