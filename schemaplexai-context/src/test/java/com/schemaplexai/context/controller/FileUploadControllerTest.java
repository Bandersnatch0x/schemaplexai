package com.schemaplexai.context.controller;

import com.schemaplexai.common.context.TenantContextHolder;
import com.schemaplexai.common.result.Result;
import com.schemaplexai.context.dto.UploadResult;
import com.schemaplexai.context.service.FileScanService;
import com.schemaplexai.context.service.FileStorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FileUploadControllerTest {

    private FileScanService scanService;
    private FileStorageService storageService;
    private FileUploadController controller;

    @BeforeEach
    void setUp() {
        scanService = mock(FileScanService.class);
        storageService = mock(FileStorageService.class);
        controller = new FileUploadController(scanService, storageService);
        TenantContextHolder.setTenantId("t1");
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void uploadCleanFile_shouldReturnSuccess() throws Exception {
        when(scanService.isHealthy()).thenReturn(true);
        when(storageService.upload(any(), any(), any(), any(), anyLong())).thenReturn("http://minio/t1/file.txt");

        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "hello".getBytes());
        Result<UploadResult> result = controller.upload(file);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("test.txt", result.getData().getName());
        verify(scanService).scan(any(ByteArrayInputStream.class), eq("test.txt"));
    }

    @Test
    void uploadWhenScanUnhealthy_shouldReturn503() throws Exception {
        when(scanService.isHealthy()).thenReturn(false);

        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "hello".getBytes());
        Result<UploadResult> result = controller.upload(file);

        assertEquals(503, result.getCode());
    }

    @Test
    void uploadEmptyFile_shouldReturn400() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", new byte[0]);
        Result<UploadResult> result = controller.upload(file);

        assertEquals(400, result.getCode());
    }

    @Test
    void scanStatus_whenHealthy_shouldReturnHealthy() {
        when(scanService.isHealthy()).thenReturn(true);
        Result<FileUploadController.ScanStatusDto> result = controller.scanStatus();
        assertTrue(result.getData().isHealthy());
    }
}
