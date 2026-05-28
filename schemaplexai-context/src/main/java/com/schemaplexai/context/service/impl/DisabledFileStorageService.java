package com.schemaplexai.context.service.impl;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.context.service.FileStorageService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.InputStream;

/**
 * Fallback storage service used when no object-storage implementation is enabled.
 */
@Service
@ConditionalOnProperty(name = "minio.enabled", havingValue = "false", matchIfMissing = true)
public class DisabledFileStorageService implements FileStorageService {

    @Override
    public String upload(String tenantId, String fileName, String contentType, InputStream inputStream, long size) {
        throw new BaseException(ResultCode.INTERNAL_ERROR, "File storage is disabled");
    }
}
