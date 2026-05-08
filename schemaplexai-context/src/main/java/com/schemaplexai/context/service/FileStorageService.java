package com.schemaplexai.context.service;

import java.io.InputStream;

/**
 * Abstraction over object storage (MinIO) for file uploads.
 */
public interface FileStorageService {

    /**
     * Upload a file to object storage under the given tenant bucket/key.
     *
     * @param tenantId    tenant identifier
     * @param fileName    original file name
     * @param contentType MIME type
     * @param inputStream file content stream
     * @param size        file size in bytes
     * @return public or pre-signed URL to access the file
     */
    String upload(String tenantId, String fileName, String contentType, InputStream inputStream, long size);
}
