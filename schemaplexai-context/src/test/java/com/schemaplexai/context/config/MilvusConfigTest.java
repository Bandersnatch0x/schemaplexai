package com.schemaplexai.context.config;

import io.milvus.v2.client.MilvusClientV2;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class MilvusConfigTest {

    @Test
    void milvusClient_initializesWithHostAndPort() {
        MilvusProperties props = new MilvusProperties();
        props.setHost("localhost");
        props.setPort(19530);
        props.setDatabaseName("default");

        MilvusConfig config = new MilvusConfig(props);

        try (MockedConstruction<MilvusClientV2> mocked = mockConstruction(MilvusClientV2.class)) {
            MilvusClientV2 client = config.milvusClient();
            assertThat(client).isNotNull();
            assertThat(mocked.constructed()).hasSize(1);
        }
    }

    @Test
    void milvusClient_includesTokenWhenPresent() {
        MilvusProperties props = new MilvusProperties();
        props.setHost("localhost");
        props.setPort(19530);
        props.setToken("root:Milvus");
        props.setDatabaseName("default");

        MilvusConfig config = new MilvusConfig(props);

        try (MockedConstruction<MilvusClientV2> mocked = mockConstruction(MilvusClientV2.class)) {
            MilvusClientV2 client = config.milvusClient();
            assertThat(client).isNotNull();
            assertThat(mocked.constructed()).hasSize(1);
        }
    }
}
