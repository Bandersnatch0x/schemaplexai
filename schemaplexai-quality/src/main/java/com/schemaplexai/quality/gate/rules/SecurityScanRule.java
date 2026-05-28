package com.schemaplexai.quality.gate.rules;

import com.schemaplexai.quality.gate.QualityCheckResult;
import com.schemaplexai.quality.gate.QualityContext;
import com.schemaplexai.quality.gate.QualityRule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;

@Slf4j
@Component
public class SecurityScanRule implements QualityRule {

    @Override
    public String getRuleName() {
        return "SECURITY_SCAN";
    }

    @Override
    public QualityCheckResult check(QualityContext context) {
        log.info("Running security scan for execution {}", context.getExecutionId());

        Map<String, Object> metadata = context.getMetadata();
        String output = metadata != null ? (String) metadata.get("output") : null;

        if (output != null && (output.contains("password=") || output.contains("secret="))) {
            return QualityCheckResult.fail("CRITICAL", "Potential secret leak detected in output");
        }

        if (metadata == null) {
            return QualityCheckResult.fail("HIGH", "Missing security scan evidence");
        }

        Object findings = metadata.get("securityScanFindings");
        if (hasFindings(findings)) {
            return QualityCheckResult.fail("CRITICAL", "Security scan reported findings");
        }

        if (isFalse(metadata.get("securityScanPassed"))) {
            return QualityCheckResult.fail("HIGH", "Security scan failed");
        }

        if (isTrue(metadata.get("securityScanCompleted")) || isTrue(metadata.get("securityScanPassed"))) {
            return QualityCheckResult.pass();
        }

        return QualityCheckResult.fail("HIGH", "Missing security scan evidence");
    }

    private boolean hasFindings(Object findings) {
        if (findings == null) {
            return false;
        }
        if (findings instanceof Collection<?> collection) {
            return !collection.isEmpty();
        }
        if (findings.getClass().isArray()) {
            return Array.getLength(findings) > 0;
        }
        if (findings instanceof String value) {
            String trimmed = value.trim();
            return !trimmed.isEmpty() && !"[]".equals(trimmed);
        }
        return true;
    }

    private boolean isTrue(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String string) {
            return Boolean.parseBoolean(string);
        }
        return false;
    }

    private boolean isFalse(Object value) {
        if (value instanceof Boolean bool) {
            return !bool;
        }
        if (value instanceof String string) {
            return "false".equalsIgnoreCase(string.trim());
        }
        return false;
    }
}
