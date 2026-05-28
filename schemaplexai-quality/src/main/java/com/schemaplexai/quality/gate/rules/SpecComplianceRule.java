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
public class SpecComplianceRule implements QualityRule {

    @Override
    public String getRuleName() {
        return "SPEC_COMPLIANCE";
    }

    @Override
    public QualityCheckResult check(QualityContext context) {
        log.info("Checking spec compliance for execution {}", context.getExecutionId());

        Map<String, Object> metadata = context.getMetadata();
        Object specId = metadata != null ? metadata.get("specId") : null;
        if (specId == null) {
            return QualityCheckResult.fail("MEDIUM", "No spec associated with execution");
        }

        Object violations = metadata.get("specComplianceViolations");
        if (hasViolations(violations)) {
            return QualityCheckResult.fail("HIGH", "Spec compliance violations found");
        }

        if (Boolean.FALSE.equals(metadata.get("specCompliancePassed"))) {
            return QualityCheckResult.fail("HIGH", "Output is not compliant with spec");
        }

        boolean checked = Boolean.TRUE.equals(metadata.get("specComplianceChecked"));
        boolean passed = Boolean.TRUE.equals(metadata.get("specCompliancePassed"));
        if (!checked || !passed) {
            return QualityCheckResult.fail("MEDIUM", "Spec compliance evidence is missing or incomplete");
        }

        return QualityCheckResult.pass();
    }

    private boolean hasViolations(Object violations) {
        if (violations == null) {
            return false;
        }
        if (violations instanceof Collection<?> collection) {
            return !collection.isEmpty();
        }
        if (violations.getClass().isArray()) {
            return Array.getLength(violations) > 0;
        }
        if (violations instanceof String value) {
            return !value.isBlank();
        }
        return true;
    }
}
