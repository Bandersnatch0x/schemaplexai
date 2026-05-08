package com.schemaplexai.context.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UploadResult {
    private String id;
    private String name;
    private String url;
    private String mimeType;
    private long size;
}
