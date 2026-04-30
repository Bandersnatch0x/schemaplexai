package com.schemaplexai.quality.gate;

public interface QualityRule {

    String getRuleName();

    QualityCheckResult check(QualityContext context);
}
