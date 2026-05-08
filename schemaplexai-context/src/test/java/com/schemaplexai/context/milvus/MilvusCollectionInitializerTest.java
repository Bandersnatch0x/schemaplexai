package com.schemaplexai.context.milvus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.context.config.MilvusProperties;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.HasCollectionReq;
import io.milvus.v2.service.index.request.CreateIndexReq;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MilvusCollectionInitializerTest {

    @Mock
    private MilvusClientV2 milvusClient;

    @Mock
    private MilvusProperties milvusProperties;

    @InjectMocks
    private MilvusCollectionInitializer initializer;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        // Re-inject objectMapper since it's final and @InjectMocks doesn't handle it well
        try {
            java.lang.reflect.Field field = MilvusCollectionInitializer.class.getDeclaredField("objectMapper");
            field.setAccessible(true);
            field.set(initializer, objectMapper);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void init_collectionAlreadyExists_skipsCreation() {
        when(milvusProperties.getCollectionName()).thenReturn("test_collection");
        when(milvusClient.hasCollection(any(HasCollectionReq.class))).thenReturn(true);

        initializer.init();

        verify(milvusClient).hasCollection(any(HasCollectionReq.class));
        verify(milvusClient, never()).createCollection(any(CreateCollectionReq.class));
        verify(milvusClient, never()).createIndex(any(CreateIndexReq.class));
    }

    @Test
    void init_collectionDoesNotExist_createsCollectionAndIndexes() {
        when(milvusProperties.getCollectionName()).thenReturn("test_collection");
        when(milvusClient.hasCollection(any(HasCollectionReq.class))).thenReturn(false);
        doNothing().when(milvusClient).createCollection(any(CreateCollectionReq.class));
        doNothing().when(milvusClient).createIndex(any(CreateIndexReq.class));

        initializer.init();

        verify(milvusClient).hasCollection(any(HasCollectionReq.class));
        verify(milvusClient).createCollection(any(CreateCollectionReq.class));
        verify(milvusClient, atLeastOnce()).createIndex(any(CreateIndexReq.class));
    }

    @Test
    void init_hasCollectionThrowsException_propagatesAsRuntimeException() {
        when(milvusProperties.getCollectionName()).thenReturn("test_collection");
        when(milvusClient.hasCollection(any(HasCollectionReq.class)))
                .thenThrow(new RuntimeException("connection refused"));

        assertThatThrownBy(() -> initializer.init())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Milvus collection initialization failed")
                .hasCauseInstanceOf(RuntimeException.class);
    }

    @Test
    void init_createCollectionThrowsException_propagatesAsRuntimeException() {
        when(milvusProperties.getCollectionName()).thenReturn("test_collection");
        when(milvusClient.hasCollection(any(HasCollectionReq.class))).thenReturn(false);
        doThrow(new RuntimeException("create failed")).when(milvusClient).createCollection(any(CreateCollectionReq.class));

        assertThatThrownBy(() -> initializer.init())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Milvus collection initialization failed");
    }

    @Test
    void init_usesCorrectCollectionName() {
        when(milvusProperties.getCollectionName()).thenReturn("my_collection");
        when(milvusClient.hasCollection(any(HasCollectionReq.class))).thenReturn(true);

        initializer.init();

        ArgumentCaptor<HasCollectionReq> captor = ArgumentCaptor.forClass(HasCollectionReq.class);
        verify(milvusClient).hasCollection(captor.capture());
        assertThat(captor.getValue().getCollectionName()).isEqualTo("my_collection");
    }

    @Test
    void init_collectionCreatedFromSchemaFile() {
        when(milvusProperties.getCollectionName()).thenReturn("test_collection");
        when(milvusClient.hasCollection(any(HasCollectionReq.class))).thenReturn(false);
        doNothing().when(milvusClient).createCollection(any(CreateCollectionReq.class));
        doNothing().when(milvusClient).createIndex(any(CreateIndexReq.class));

        initializer.init();

        verify(milvusClient).hasCollection(any(HasCollectionReq.class));
        verify(milvusClient).createCollection(any(CreateCollectionReq.class));
        verify(milvusClient, atLeastOnce()).createIndex(any(CreateIndexReq.class));
    }
}
