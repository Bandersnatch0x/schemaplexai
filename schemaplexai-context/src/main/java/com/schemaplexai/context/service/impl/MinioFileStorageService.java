package com.schemaplexai.context.service.impl;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.context.service.FileStorageService;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Service
@ConditionalOnProperty(name = "minio.enabled", havingValue = "true", matchIfMissing = false)
public class MinioFileStorageService implements FileStorageService {

    @Value("${minio.endpoint:http://localhost:9000}")
    private String endpoint;

    @Value("${minio.access-key:}")
    private String accessKey;

    @Value("${minio.secret-key:}")
    private String secretKey;

    @Value("${minio.bucket:documents}")
    private String bucket;

    private MinioClient minioClient;

    @PostConstruct
    void init() {
        if (accessKey == null || accessKey.isBlank() || secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("MinIO credentials missing");
        }
        minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
        log.info("MinIO file storage initialized: endpoint={}, bucket={}", endpoint, bucket);
    }

    @Override
    public String upload(String tenantId, String fileName, String contentType, InputStream inputStream, long size) {
        String objectName = tenantId + "/" + UUID.randomUUID() + "-" + fileName;
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .contentType(contentType)
                            .stream(inputStream, size, -1)
                            .build()
            );
            String url = endpoint + "/" + bucket + "/" + objectName;
            log.info("Uploaded file to MinIO: {}", url);
            return url;
        } catch (Exception e) {
            log.error("MinIO upload failed for {}", fileName, e);
            throw new BaseException(ResultCode.INTERNAL_ERROR, "File upload failed: " + e.getMessage());
        }
    }
}
