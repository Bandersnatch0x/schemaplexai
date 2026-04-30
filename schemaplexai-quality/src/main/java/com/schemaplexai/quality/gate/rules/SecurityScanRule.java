package com.schemaplexai.quality.gate.rules;

import com.schemaplexai.quality.gate.QualityCheckResult;
import com.schemaplexai.quality.gate.QualityContext;
import com.schemaplexai.quality.gate.QualityRule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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

        // Phase 1: Placeholder — actual security scan would check for secrets, injection, etc.
        String output = context.getMetadata() != null ?
                (String) context.getMetadata().get("output") : null;

        if (output != null && (output.contains("password=") || output.contains("secret="))) {
            return QualityCheckResult.fail("CRITICAL", "Potential secret leak detected in output");
        }

        return QualityCheckResult.pass();
    }
}
