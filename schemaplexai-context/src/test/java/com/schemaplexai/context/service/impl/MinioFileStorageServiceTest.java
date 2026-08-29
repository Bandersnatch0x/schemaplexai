package com.schemaplexai.context.service.impl;

import com.schemaplexai.common.exception.BaseException;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MinioFileStorageServiceTest {

    @Mock
    private MinioClient minioClient;

    private MinioFileStorageService service;

    @BeforeEach
    void setUp() {
        service = new MinioFileStorageService();
        ReflectionTestUtils.setField(service, "endpoint", "http://localhost:9000");
        ReflectionTestUtils.setField(service, "accessKey", "test-key");
        ReflectionTestUtils.setField(service, "secretKey", "test-secret");
        ReflectionTestUtils.setField(service, "defaultBucket", "documents");
        ReflectionTestUtils.setField(service, "minioClient", minioClient);
    }

    @Test
    void resolveBucket_withTenantId_returnsTenantBucket() {
        assertThat(service.resolveBucket("tenant-1")).isEqualTo("sf-files-tenant-1");
    }

    @Test
    void resolveBucket_withNullTenant_returnsDefaultBucket() {
        assertThat(service.resolveBucket(null)).isEqualTo("documents");
    }

    @Test
    void resolveBucket_withBlankTenant_returnsDefaultBucket() {
        assertThat(service.resolveBucket("  ")).isEqualTo("documents");
    }

    @Test
    void ensureBucketExists_alreadyCached_skipsCheck() throws Exception {
        // First call — cache miss, checks bucket
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
        service.ensureBucketExists("sf-files-t1");
        verify(minioClient).bucketExists(any(BucketExistsArgs.class));

        // Second call — cache hit, no interaction with client
        reset(minioClient);
        service.ensureBucketExists("sf-files-t1");
        verifyNoInteractions(minioClient);
    }

    @Test
    void ensureBucketExists_bucketMissing_createsBucket() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);

        service.ensureBucketExists("new-bucket");

        verify(minioClient).makeBucket(any(MakeBucketArgs.class));
    }

    @Test
    void ensureBucketExists_bucketExists_doesNotCreate() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

        service.ensureBucketExists("existing-bucket");

        verify(minioClient, never()).makeBucket(any(MakeBucketArgs.class));
    }

    @Test
    void ensureBucketExists_clientError_throwsBaseException() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class)))
                .thenThrow(new RuntimeException("connection refused"));

        assertThatThrownBy(() -> service.ensureBucketExists("fail-bucket"))
                .isInstanceOf(BaseException.class);
    }

    @Test
    void upload_success_returnsUrl() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

        String result = service.upload("t1", "file.txt", "text/plain",
                new ByteArrayInputStream("data".getBytes()), 4);

        assertThat(result).startsWith("http://localhost:9000/sf-files-t1/");
        assertThat(result).contains("file.txt");
        verify(minioClient).putObject(any(PutObjectArgs.class));
    }

    @Test
    void upload_clientError_throwsBaseException() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
        doThrow(new RuntimeException("upload failed")).when(minioClient).putObject(any(PutObjectArgs.class));

        assertThatThrownBy(() -> service.upload("t1", "file.txt", "text/plain",
                new ByteArrayInputStream("data".getBytes()), 4))
                .isInstanceOf(BaseException.class);
    }

    @Test
    void getMinioClient_returnsInjectedClient() {
        assertThat(service.getMinioClient()).isSameAs(minioClient);
    }
}
