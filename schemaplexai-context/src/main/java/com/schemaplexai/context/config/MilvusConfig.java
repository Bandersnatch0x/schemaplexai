package com.schemaplexai.context.config;

import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@EnableConfigurationProperties(MilvusProperties.class)
@RequiredArgsConstructor
@ConditionalOnProperty(name = "milvus.enabled", havingValue = "true", matchIfMissing = false)
public class MilvusConfig {

    private final MilvusProperties milvusProperties;

    @Bean
    public MilvusClientV2 milvusClient() {
        ConnectConfig.ConnectConfigBuilder builder = ConnectConfig.builder()
                .uri("http://" + milvusProperties.getHost() + ":" + milvusProperties.getPort());

        if (milvusProperties.getToken() != null && !milvusProperties.getToken().isBlank()) {
            builder.token(milvusProperties.getToken());
        }

        MilvusClientV2 client = new MilvusClientV2(builder.build());
        log.info("MilvusClientV2 initialized: {}:{}, database: {}",
                milvusProperties.getHost(),
                milvusProperties.getPort(),
                milvusProperties.getDatabaseName());
        return client;
    }
}
