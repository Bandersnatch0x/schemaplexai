package com.schemaplexai.context.service.impl;

import com.schemaplexai.context.service.FileScanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.InputStream;

/**
 * No-op scan service used when ClamAV is disabled.
 * Always reports healthy but performs no actual scanning.
 * NOT recommended for production.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "clamav.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpFileScanService implements FileScanService {

    @Override
    public void scan(InputStream inputStream, String fileName) {
        log.warn("No-op virus scan for '{}'; ClamAV is disabled", fileName);
    }

    @Override
    public boolean isHealthy() {
        return true;
    }
}
