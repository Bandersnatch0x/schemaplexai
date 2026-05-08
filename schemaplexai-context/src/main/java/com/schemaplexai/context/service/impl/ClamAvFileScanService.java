package com.schemaplexai.context.service.impl;

import com.schemaplexai.context.exception.ScanServiceException;
import com.schemaplexai.context.exception.VirusDetectedException;
import com.schemaplexai.context.service.FileScanService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@ConditionalOnProperty(name = "clamav.enabled", havingValue = "true", matchIfMissing = false)
public class ClamAvFileScanService implements FileScanService {

    private static final int CHUNK_SIZE = 2048;
    private static final int CONNECTION_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 30000;
    private static final byte[] INSTREAM_HEADER = "zINSTREAM\n".getBytes(StandardCharsets.UTF_8);
    private static final byte[] ZERO_LENGTH_CHUNK = new byte[]{0, 0, 0, 0};

    @Value("${clamav.host:localhost}")
    private String host;

    @Value("${clamav.port:3310}")
    private int port;

    private volatile boolean healthy = false;

    @PostConstruct
    void checkHealth() {
        healthy = ping();
        if (healthy) {
            log.info("ClamAV scan service is healthy at {}:{}", host, port);
        } else {
            log.warn("ClamAV scan service is NOT reachable at {}:{}", host, port);
        }
    }

    @Override
    public void scan(InputStream inputStream, String fileName) {
        if (!healthy) {
            throw new ScanServiceException("ClamAV is not healthy; upload disabled");
        }

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), CONNECTION_TIMEOUT_MS);
            socket.setSoTimeout(READ_TIMEOUT_MS);

            try (OutputStream out = socket.getOutputStream();
                 InputStream in = socket.getInputStream()) {

                out.write(INSTREAM_HEADER);
                out.flush();

                byte[] buffer = new byte[CHUNK_SIZE];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    writeChunk(out, buffer, read);
                }
                out.write(ZERO_LENGTH_CHUNK);
                out.flush();

                String response = readResponse(in);
                log.debug("ClamAV response for '{}': {}", fileName, response);

                if (response.contains("FOUND")) {
                    String virus = extractVirusName(response);
                    log.error("Virus detected in '{}': {}", fileName, virus);
                    throw new VirusDetectedException(fileName, virus);
                }

                if (!response.contains("OK")) {
                    throw new ScanServiceException("Unexpected ClamAV response: " + response);
                }
            }
        } catch (VirusDetectedException | ScanServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("ClamAV scan failed for '{}'", fileName, e);
            throw new ScanServiceException("Scan failed for " + fileName, e);
        }
    }

    @Override
    public boolean isHealthy() {
        return healthy;
    }

    private boolean ping() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), CONNECTION_TIMEOUT_MS);
            socket.setSoTimeout(READ_TIMEOUT_MS);
            try (OutputStream out = socket.getOutputStream();
                 InputStream in = socket.getInputStream()) {
                out.write("zPING\n".getBytes(StandardCharsets.UTF_8));
                out.flush();
                String response = readResponse(in);
                return response.contains("PONG");
            }
        } catch (Exception e) {
            log.warn("ClamAV ping failed: {}", e.getMessage());
            return false;
        }
    }

    private void writeChunk(OutputStream out, byte[] buffer, int length) throws IOException {
        out.write((length >>> 24) & 0xFF);
        out.write((length >>> 16) & 0xFF);
        out.write((length >>> 8) & 0xFF);
        out.write(length & 0xFF);
        out.write(buffer, 0, length);
    }

    private String readResponse(InputStream in) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[256];
        int read;
        while ((read = in.read(buf)) != -1) {
            baos.write(buf, 0, read);
            if (baos.toString(StandardCharsets.UTF_8).endsWith("\n")) {
                break;
            }
        }
        return baos.toString(StandardCharsets.UTF_8).trim();
    }

    private String extractVirusName(String response) {
        int idx = response.lastIndexOf(':');
        if (idx != -1 && idx + 1 < response.length()) {
            String after = response.substring(idx + 1).trim();
            if (after.endsWith(" FOUND")) {
                return after.substring(0, after.length() - " FOUND".length()).trim();
            }
            return after;
        }
        return "unknown";
    }
}
