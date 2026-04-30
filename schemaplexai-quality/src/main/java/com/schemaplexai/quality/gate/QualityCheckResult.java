package com.schemaplexai.quality.gate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QualityCheckResult {

    private boolean passed;
    private String severity; // CRITICAL, HIGH, MEDIUM, LOW
    private String message;

    public static QualityCheckResult pass() {
        return new QualityCheckResult(true, null, null);
    }

    public static QualityCheckResult fail(String severity, String message) {
        return new QualityCheckResult(false, severity, message);
    }
}
