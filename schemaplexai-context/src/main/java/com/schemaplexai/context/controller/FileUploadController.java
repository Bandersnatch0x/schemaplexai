package com.schemaplexai.context.controller;

import com.schemaplexai.common.context.TenantContextHolder;
import com.schemaplexai.common.result.Result;
import com.schemaplexai.context.dto.UploadResult;
import com.schemaplexai.context.service.FileScanService;
import com.schemaplexai.context.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/context/files")
@RequiredArgsConstructor
@Tag(name = "文件上传", description = "文件上传、病毒扫描与租户配额校验")
public class FileUploadController {

    private final FileScanService fileScanService;
    private final FileStorageService fileStorageService;

    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB
    private static final long MAX_TOTAL_SIZE_PER_TENANT = 1024L * 1024 * 1024; // 1GB

    @PostMapping("/upload")
    @Operation(summary = "上传文件（含病毒扫描）")
    public Result<UploadResult> upload(@RequestParam("file") MultipartFile file) throws IOException {
        String tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            return Result.error(400, "Tenant ID missing");
        }

        if (file.isEmpty()) {
            return Result.error(400, "File is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            return Result.error(400, "File exceeds 50MB limit");
        }

        if (!fileScanService.isHealthy()) {
            return Result.error(503, "File scan service unavailable; upload disabled");
        }

        byte[] bytes = file.getBytes();
        fileScanService.scan(new ByteArrayInputStream(bytes), file.getOriginalFilename());

        String url = fileStorageService.upload(
                tenantId,
                file.getOriginalFilename(),
                file.getContentType(),
                new ByteArrayInputStream(bytes),
                file.getSize()
        );

        UploadResult result = UploadResult.builder()
                .id(UUID.randomUUID().toString())
                .name(file.getOriginalFilename())
                .url(url)
                .mimeType(file.getContentType())
                .size(file.getSize())
                .build();

        return Result.success(result);
    }

    @GetMapping("/scan-status")
    @Operation(summary = "查询病毒扫描服务健康状态")
    public Result<ScanStatusDto> scanStatus() {
        boolean healthy = fileScanService.isHealthy();
        ScanStatusDto dto = new ScanStatusDto();
        dto.setHealthy(healthy);
        dto.setMessage(healthy ? "ClamAV is ready" : "ClamAV is not reachable");
        return Result.success(dto);
    }

    public static class ScanStatusDto {
        private boolean healthy;
        private String message;

        public boolean isHealthy() { return healthy; }
        public void setHealthy(boolean healthy) { this.healthy = healthy; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
