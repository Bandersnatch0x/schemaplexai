package com.schemaplexai.context.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MilvusPropertiesTest {

    @Test
    void defaultValues() {
        MilvusProperties props = new MilvusProperties();
        assertThat(props.getHost()).isEqualTo("localhost");
        assertThat(props.getPort()).isEqualTo(19530);
        assertThat(props.getDatabaseName()).isEqualTo("default");
        assertThat(props.getCollectionName()).isEqualTo("knowledge_doc_embedding");
    }

    @Test
    void settersAndGetters() {
        MilvusProperties props = new MilvusProperties();
        props.setHost("milvus.example.com");
        props.setPort(19531);
        props.setToken("abc");
        props.setDatabaseName("db1");
        props.setCollectionName("col1");

        assertThat(props.getHost()).isEqualTo("milvus.example.com");
        assertThat(props.getPort()).isEqualTo(19531);
        assertThat(props.getToken()).isEqualTo("abc");
        assertThat(props.getDatabaseName()).isEqualTo("db1");
        assertThat(props.getCollectionName()).isEqualTo("col1");
    }
}
