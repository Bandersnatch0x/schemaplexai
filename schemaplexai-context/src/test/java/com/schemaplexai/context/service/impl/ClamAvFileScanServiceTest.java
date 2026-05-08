package com.schemaplexai.context.service.impl;

import com.schemaplexai.context.exception.ScanServiceException;
import com.schemaplexai.context.exception.VirusDetectedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ClamAvFileScanServiceTest {

    private ClamAvFileScanService service;

    @BeforeEach
    void setUp() {
        service = new ClamAvFileScanService();
    }

    @Test
    void scanCleanFile_shouldPass() throws Exception {
        int port = startMockClamAv("stream: OK\n");
        injectConfig("localhost", port);
        service.checkHealth();

        assertDoesNotThrow(() ->
                service.scan(new ByteArrayInputStream("clean content".getBytes()), "test.txt")
        );
    }

    @Test
    void scanInfectedFile_shouldThrowVirusDetectedException() throws Exception {
        int port = startMockClamAv("stream: Eicar-Test-Signature FOUND\n");
        injectConfig("localhost", port);
        service.checkHealth();

        VirusDetectedException ex = assertThrows(VirusDetectedException.class, () ->
                service.scan(new ByteArrayInputStream("X5O!P%@AP[4\\PZX54(P^)7CC)7}$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!$H+H*".getBytes()), "eicar.com")
        );
        assertTrue(ex.getMessage().contains("Eicar-Test-Signature"));
    }

    @Test
    void scanWhenUnhealthy_shouldThrowScanServiceException() {
        injectConfig("localhost", 1);
        service.checkHealth();
        assertFalse(service.isHealthy());

        ScanServiceException ex = assertThrows(ScanServiceException.class, () ->
                service.scan(new ByteArrayInputStream("any".getBytes()), "any.txt")
        );
        assertTrue(ex.getMessage().contains("not healthy"));
    }

    private void injectConfig(String host, int port) {
        try {
            var hostField = ClamAvFileScanService.class.getDeclaredField("host");
            hostField.setAccessible(true);
            hostField.set(service, host);
            var portField = ClamAvFileScanService.class.getDeclaredField("port");
            portField.setAccessible(true);
            portField.set(service, port);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Starts a mock ClamAV server that handles zPING and zINSTREAM on separate connections.
     * For INSTREAM, it reads the 4-byte length-prefixed chunks directly from the InputStream
     * and responds after the zero-length chunk.
     */
    private int startMockClamAv(String response) throws IOException, InterruptedException {
        ServerSocket serverSocket = new ServerSocket(0);
        int port = serverSocket.getLocalPort();
        CountDownLatch latch = new CountDownLatch(1);
        new Thread(() -> {
            try {
                while (!serverSocket.isClosed()) {
                    Socket socket = serverSocket.accept();
                    new Thread(() -> handleClient(socket, response, latch)).start();
                }
            } catch (IOException e) {
                // ignore
            }
        }).start();
        latch.await(1, TimeUnit.SECONDS);
        return port;
    }

    private void handleClient(Socket socket, String response, CountDownLatch latch) {
        try (OutputStream out = socket.getOutputStream();
             var in = socket.getInputStream()) {
            latch.countDown();
            // Read the command line (e.g., "zPING\n" or "zINSTREAM\n")
            StringBuilder cmdBuilder = new StringBuilder();
            int b;
            while ((b = in.read()) != -1) {
                cmdBuilder.append((char) b);
                if (b == '\n') break;
            }
            String cmd = cmdBuilder.toString().trim();
            if (cmd.equals("zPING")) {
                out.write("PONG\n".getBytes(StandardCharsets.UTF_8));
                out.flush();
            } else if (cmd.equals("zINSTREAM")) {
                // Read 4-byte length prefixed chunks
                while (true) {
                    int len = readInt32(in);
                    if (len <= 0) break;
                    long skipped = 0;
                    while (skipped < len) {
                        long s = in.skip(len - skipped);
                        if (s <= 0) break;
                        skipped += s;
                    }
                }
                out.write(response.getBytes(StandardCharsets.UTF_8));
                out.flush();
            }
        } catch (IOException e) {
            // ignore
        }
    }

    private int readInt32(java.io.InputStream in) throws IOException {
        int b1 = in.read();
        int b2 = in.read();
        int b3 = in.read();
        int b4 = in.read();
        if (b1 == -1 || b2 == -1 || b3 == -1 || b4 == -1) return -1;
        return ((b1 & 0xFF) << 24) | ((b2 & 0xFF) << 16) | ((b3 & 0xFF) << 8) | (b4 & 0xFF);
    }
}
