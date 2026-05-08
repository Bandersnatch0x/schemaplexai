package com.schemaplexai.context.service.impl;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.context.service.FileStorageService;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MinIO file storage with bucket-per-tenant isolation.
 * <p>
 * Each tenant gets its own bucket named {@code sf-files-{tenantId}}.
 * Buckets are auto-created on first access. When no tenant context is
 * available, falls back to the configured default bucket.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "minio.enabled", havingValue = "true", matchIfMissing = false)
public class MinioFileStorageService implements FileStorageService {

    private static final String TENANT_BUCKET_PREFIX = "sf-files-";

    @Value("${minio.endpoint:http://localhost:9000}")
    private String endpoint;

    @Value("${minio.access-key:}")
    private String accessKey;

    @Value("${minio.secret-key:}")
    private String secretKey;

    @Value("${minio.bucket:documents}")
    private String defaultBucket;

    private MinioClient minioClient;

    /** Cache of known-existing buckets to avoid repeated BucketExists calls. */
    private final Set<String> existingBuckets = ConcurrentHashMap.newKeySet();

    @PostConstruct
    void init() {
        if (accessKey == null || accessKey.isBlank() || secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("MinIO credentials missing");
        }
        minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
        log.info("MinIO file storage initialized: endpoint={}, defaultBucket={}", endpoint, defaultBucket);
    }

    @Override
    public String upload(String tenantId, String fileName, String contentType, InputStream inputStream, long size) {
        String bucket = resolveBucket(tenantId);
        String objectName = UUID.randomUUID() + "-" + fileName;
        try {
            ensureBucketExists(bucket);
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .contentType(contentType)
                            .stream(inputStream, size, -1)
                            .build()
            );
            String url = endpoint + "/" + bucket + "/" + objectName;
            log.info("Uploaded file to MinIO: bucket={}, object={}", bucket, objectName);
            return url;
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("MinIO upload failed for {}", fileName, e);
            throw new BaseException(ResultCode.INTERNAL_ERROR, "File upload failed: " + e.getMessage());
        }
    }

    /**
     * Resolve the bucket name for a given tenant.
     * Returns tenant-scoped bucket {@code sf-files-{tenantId}} when tenantId is present,
     * otherwise falls back to the configured default bucket.
     */
    String resolveBucket(String tenantId) {
        if (tenantId != null && !tenantId.isBlank()) {
            return TENANT_BUCKET_PREFIX + tenantId;
        }
        return defaultBucket;
    }

    /**
     * Ensure the given bucket exists, creating it if necessary.
     * Uses an in-memory cache to avoid repeated round-trips for known buckets.
     */
    void ensureBucketExists(String bucket) {
        if (existingBuckets.contains(bucket)) {
            return;
        }
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("Auto-created MinIO bucket: {}", bucket);
            }
            existingBuckets.add(bucket);
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to ensure bucket '{}' exists: {}", bucket, e.getMessage(), e);
            throw new BaseException(ResultCode.INTERNAL_ERROR, "Bucket check/create failed: " + e.getMessage());
        }
    }

    // Visible for testing
    MinioClient getMinioClient() {
        return minioClient;
    }
}
