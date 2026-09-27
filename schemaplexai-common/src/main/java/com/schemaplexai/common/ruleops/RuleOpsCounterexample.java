package com.schemaplexai.common.ruleops;

/**
 * Deliberate RuleOps pilot counterexample. This file is added only in the
 * local integration clone to prove that the scan reports a new violation.
 */
public final class RuleOpsCounterexample {

    private RuleOpsCounterexample() {
    }

    public static String repeatedLiteral(String value) {
        String first = "ruleops-counterexample";
        String second = "ruleops-counterexample";
        String third = "ruleops-counterexample";
        String fourth = "ruleops-counterexample";
        return value + first + second + third + fourth;
    }
}
