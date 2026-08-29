package com.schemaplexai.task.config;

import com.schemaplexai.task.service.ContextServiceClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Builds the {@link ContextServiceClient} with explicit connect/read timeouts —
 * cross-service calls must never hang indefinitely. The read timeout is generous
 * because a sync re-runs the full pipeline (MinIO download, Tika extraction,
 * embedding generation, Milvus insert) for potentially large documents.
 */
@Configuration
public class ContextServiceClientConfig {

    @Bean
    public ContextServiceClient contextServiceClient(
            @Value("${task.context-service.base-url:http://localhost:8085}") String baseUrl,
            @Value("${task.context-service.connect-timeout-ms:5000}") int connectTimeoutMs,
            @Value("${task.context-service.read-timeout-ms:300000}") int readTimeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMs);
        requestFactory.setReadTimeout(readTimeoutMs);
        RestClient restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
        return new ContextServiceClient(restClient);
    }
}
