package com.schemaplexai.agent.engine.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TokenEstimator")
class TokenEstimatorTest {

    @Nested
    @DisplayName("estimate")
    class EstimateTests {

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("should return 0 for null or empty text")
        void shouldReturnZeroForNullOrEmpty(String input) {
            assertEquals(0, TokenEstimator.estimate(input));
        }

        @Test
        @DisplayName("should return at least 1 for non-empty text")
        void shouldReturnAtLeastOne() {
            assertThat(TokenEstimator.estimate("a")).isEqualTo(1);
        }

        @Test
        @DisplayName("should estimate ~1 token per 4 characters")
        void shouldEstimateOnePerFourChars() {
            // 4 chars -> 1 token
            assertEquals(1, TokenEstimator.estimate("abcd"));
            // 8 chars -> 2 tokens
            assertEquals(2, TokenEstimator.estimate("abcdefgh"));
            // 100 chars -> 25 tokens
            assertEquals(25, TokenEstimator.estimate("a".repeat(100)));
        }

        @Test
        @DisplayName("should round down for partial token")
        void shouldRoundDown() {
            // 5 chars -> 5/4 = 1 (integer division)
            assertEquals(1, TokenEstimator.estimate("abcde"));
            // 7 chars -> 7/4 = 1
            assertEquals(1, TokenEstimator.estimate("abcdefg"));
            // 3 chars -> 3/4 = 0, but min 1
            assertEquals(1, TokenEstimator.estimate("abc"));
        }

        @ParameterizedTest
        @CsvSource({
                "Hello, 1",
                "Hello World, 2",
                "The quick brown fox jumps over the lazy dog, 10",
                "a, 1",
                "abcdefghijklmnop, 4"
        })
        @DisplayName("should estimate correctly for various inputs")
        void shouldEstimateCorrectly(String input, long expected) {
            assertEquals(expected, TokenEstimator.estimate(input));
        }

        @Test
        @DisplayName("should handle long text")
        void shouldHandleLongText() {
            String longText = "word ".repeat(1000); // ~5000 chars
            long estimate = TokenEstimator.estimate(longText);
            assertThat(estimate).isGreaterThan(0);
            assertThat(estimate).isEqualTo(longText.length() / 4L);
        }

        @Test
        @DisplayName("should handle single character")
        void shouldHandleSingleChar() {
            assertEquals(1, TokenEstimator.estimate("x"));
        }

        @Test
        @DisplayName("should handle text with special characters")
        void shouldHandleSpecialChars() {
            String text = "Hello! @#$%^&*() 123";
            long estimate = TokenEstimator.estimate(text);
            assertEquals(text.length() / 4L, estimate);
        }
    }
}
