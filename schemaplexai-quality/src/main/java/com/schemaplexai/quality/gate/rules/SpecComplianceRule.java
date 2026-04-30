package com.schemaplexai.quality.gate.rules;

import com.schemaplexai.quality.gate.QualityCheckResult;
import com.schemaplexai.quality.gate.QualityContext;
import com.schemaplexai.quality.gate.QualityRule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SpecComplianceRule implements QualityRule {

    @Override
    public String getRuleName() {
        return "SPEC_COMPLIANCE";
    }

    @Override
    public QualityCheckResult check(QualityContext context) {
        log.info("Checking spec compliance for execution {}", context.getExecutionId());

        // Phase 1: Placeholder — actual spec compliance check to be implemented
        // This would verify that the agent output conforms to the spec requirements
        Object specId = context.getMetadata() != null ? context.getMetadata().get("specId") : null;
        if (specId == null) {
            return QualityCheckResult.fail("MEDIUM", "No spec associated with execution");
        }

        return QualityCheckResult.pass();
    }
}
