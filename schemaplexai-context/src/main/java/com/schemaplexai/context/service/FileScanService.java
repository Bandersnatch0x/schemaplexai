package com.schemaplexai.context.service;

import java.io.InputStream;

/**
 * Virus scanning service interface. Implementations must scan file content
 * before allowing upload to persistent storage.
 */
public interface FileScanService {

    /**
     * Scan the given input stream for viruses.
     *
     * @param inputStream the stream to scan (will be consumed)
     * @param fileName    original file name for logging
     * @throws VirusDetectedException if a virus is detected
     * @throws ScanServiceException   if the scan service is unavailable
     */
    void scan(InputStream inputStream, String fileName);

    /**
     * Check whether the scanning backend is healthy.
     *
     * @return true if scans can be performed
     */
    boolean isHealthy();
}
