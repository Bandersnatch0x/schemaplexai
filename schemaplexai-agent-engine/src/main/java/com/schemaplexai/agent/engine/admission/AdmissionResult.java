package com.schemaplexai.agent.engine.admission;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdmissionResult {

    private boolean allowed;
    private String reason;
    private CompressionStrategy suggestedCompression;
}
