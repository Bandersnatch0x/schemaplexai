package com.schemaplexai.agent.engine.guardrails;

import com.schemaplexai.agent.engine.evaluation.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LengthGuardrailTest {

    private LengthGuardrail defaultGuardrail;
    private LengthGuardrail strictGuardrail;

    @BeforeEach
    void setUp() {
        defaultGuardrail = new LengthGuardrail();
        strictGuardrail = new LengthGuardrail(100, 100);
    }

    // --- Default constructor tests ---

    @Test
    void defaultConstructorShouldAllowReasonableInput() {
        String input = "x".repeat(50_000);
        ValidationResult result = defaultGuardrail.validateInput(input);
        assertTrue(result.success(), "50k chars should be under default 100k limit");
    }

    @Test
    void defaultConstructorShouldBlockExcessiveInput() {
        String input = "x".repeat(150_000);
        ValidationResult result = defaultGuardrail.validateInput(input);
        assertFalse(result.success(), "150k chars should exceed default 100k limit");
        assertNotNull(result.errorMessage());
        assertTrue(result.errorMessage().contains("exceeds max"));
        assertTrue(result.errorMessage().contains("Input"));
    }

    @Test
    void defaultConstructorShouldBlockExcessiveOutput() {
        String output = "a".repeat(150_000);
        ValidationResult result = defaultGuardrail.validateOutput(output);
        assertFalse(result.success(), "150k chars should exceed default 100k limit");
        assertTrue(result.errorMessage().contains("Output"));
    }

    // --- Custom limits tests ---

    @Test
    void customLimitShouldAllowWithinBounds() {
        ValidationResult result = strictGuardrail.validateInput("short text");
        assertTrue(result.success());
    }

    @Test
    void customLimitShouldBlockOverLimitInput() {
        String input = "x".repeat(101);
        ValidationResult result = strictGuardrail.validateInput(input);
        assertFalse(result.success());
        assertTrue(result.errorMessage().contains("101"));
        assertTrue(result.errorMessage().contains("100"));
    }

    @Test
    void customLimitShouldBlockOverLimitOutput() {
        String output = "a".repeat(101);
        ValidationResult result = strictGuardrail.validateOutput(output);
        assertFalse(result.success());
        assertTrue(result.errorMessage().contains("Output"));
    }

    @Test
    void customLimitShouldAllowExactlyAtLimit() {
        String input = "x".repeat(100);
        ValidationResult result = strictGuardrail.validateInput(input);
        assertTrue(result.success(), "Exactly at limit should pass");
    }

    @Test
    void veryStrictLimitShouldBlockMostInputs() {
        LengthGuardrail veryStrict = new LengthGuardrail(5, 5);
        assertTrue(veryStrict.validateInput("hi").success());
        assertFalse(veryStrict.validateInput("hello world").success());
    }

    // --- Null handling tests ---

    @Test
    void shouldPassNullInput() {
        ValidationResult result = defaultGuardrail.validateInput(null);
        assertTrue(result.success(), "Null input should pass");
        assertNull(result.errorMessage());
    }

    @Test
    void shouldPassNullOutput() {
        ValidationResult result = defaultGuardrail.validateOutput(null);
        assertTrue(result.success(), "Null output should pass");
    }

    // --- Empty string tests ---

    @Test
    void shouldPassEmptyInput() {
        ValidationResult result = defaultGuardrail.validateInput("");
        assertTrue(result.success());
    }

    @Test
    void shouldPassEmptyOutput() {
        ValidationResult result = defaultGuardrail.validateOutput("");
        assertTrue(result.success());
    }

    // --- isHighRisk tests ---

    @Test
    void shouldNeverFlagAsHighRisk() {
        assertFalse(defaultGuardrail.isHighRisk("anything"));
        assertFalse(defaultGuardrail.isHighRisk("databaseDrop"));
        assertFalse(defaultGuardrail.isHighRisk(null));
        assertFalse(defaultGuardrail.isHighRisk(""));
    }

    // --- getName tests ---

    @Test
    void shouldReturnCorrectName() {
        assertEquals("LengthGuardrail", defaultGuardrail.getName());
    }

    // --- Boundary tests ---

    @Test
    void shouldHandleZeroLengthLimit() {
        LengthGuardrail zeroLimit = new LengthGuardrail(0, 0);
        assertFalse(zeroLimit.validateInput("a").success(), "Any non-empty input should fail with 0 limit");
        assertTrue(zeroLimit.validateInput("").success(), "Empty input should pass with 0 limit");
        assertTrue(zeroLimit.validateInput(null).success(), "Null input should pass with 0 limit");
    }

    @Test
    void shouldHandleDifferentInputAndOutputLimits() {
        LengthGuardrail asymmetric = new LengthGuardrail(10, 1000);
        assertFalse(asymmetric.validateInput("x".repeat(11)).success());
        assertTrue(asymmetric.validateOutput("x".repeat(100)).success());
    }
}
