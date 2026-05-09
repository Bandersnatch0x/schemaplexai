package com.schemaplexai.agent.engine.groupchat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConsensusDetectorTest {

    private ConsensusDetector detector;

    @BeforeEach
    void setUp() {
        detector = new ConsensusDetector();
    }

    @Test
    void explicitAgreement_allAgentsAgree() {
        List<String> messages = List.of(
                "I think we should use PostgreSQL",
                "I agree, PostgreSQL is the right choice",
                "Agreed, let's go with PostgreSQL"
        );

        ConsensusDetector.ConsensusResult result = detector.detectConsensus(messages, 2, 2);

        assertThat(result.consensusReached()).isTrue();
        assertThat(result.agreementScore()).isGreaterThan(0.0);
        assertThat(result.reason()).containsIgnoringCase("agreement");
    }

    @Test
    void explicitAgreement_partialAgreementNotEnough() {
        List<String> messages = List.of(
                "I think we should use PostgreSQL",
                "I agree",
                "I prefer MySQL actually"
        );

        ConsensusDetector.ConsensusResult result = detector.detectConsensus(messages, 3, 2);

        assertThat(result.consensusReached()).isFalse();
    }

    @Test
    void keywordOverlap_consensusOnKeywords() {
        List<String> messages = List.of(
                "We should implement caching with Redis for performance",
                "Redis caching will improve performance significantly",
                "Using Redis as cache is good performance optimization"
        );

        ConsensusDetector.ConsensusResult result = detector.detectConsensus(messages, 3, 2);

        assertThat(result.consensusReached()).isTrue();
        assertThat(result.reason()).containsIgnoringCase("overlap");
    }

    @Test
    void noConsensus_differentTopics() {
        List<String> messages = List.of(
                "We should use PostgreSQL for the database",
                "I think React is better than Vue",
                "Let's deploy on Kubernetes"
        );

        ConsensusDetector.ConsensusResult result = detector.detectConsensus(messages, 3, 2);

        assertThat(result.consensusReached()).isFalse();
        assertThat(result.agreementScore()).isLessThan(1.0);
    }

    @Test
    void insufficientMessages_returnsFalse() {
        List<String> messages = List.of("Only one message");

        ConsensusDetector.ConsensusResult result = detector.detectConsensus(messages, 3, 2);

        assertThat(result.consensusReached()).isFalse();
        assertThat(result.reason()).contains("Insufficient messages");
    }

    @Test
    void nullMessages_returnsFalse() {
        ConsensusDetector.ConsensusResult result = detector.detectConsensus(null, 3, 2);

        assertThat(result.consensusReached()).isFalse();
        assertThat(reasonOrEmpty(result)).contains("Insufficient messages");
    }

    @Test
    void singleAgentAlwaysConsensus() {
        List<String> messages = List.of("Anything");

        ConsensusDetector.ConsensusResult result = detector.detectConsensus(messages, 1, 2);

        assertThat(result.consensusReached()).isTrue();
        assertThat(result.agreementScore()).isEqualTo(1.0);
    }

    @Test
    void emptyMessages_noConsensus() {
        ConsensusDetector.ConsensusResult result = detector.detectConsensus(List.of(), 2, 2);

        assertThat(result.consensusReached()).isFalse();
    }

    @Test
    void lgtmCountsAsAgreement() {
        List<String> messages = List.of(
                "Proposed solution: use JWT tokens",
                "LGTM",
                "Looks good"
        );

        ConsensusDetector.ConsensusResult result = detector.detectConsensus(messages, 2, 2);

        assertThat(result.consensusReached()).isTrue();
    }

    @Test
    void approvedCountsAsAgreement() {
        List<String> messages = List.of(
                "Let's refactor the service layer",
                "Approved",
                "approved by me"
        );

        ConsensusDetector.ConsensusResult result = detector.detectConsensus(messages, 2, 2);

        assertThat(result.consensusReached()).isTrue();
    }

    @Test
    void defaultThresholdOverload() {
        List<String> messages = List.of(
                "I agree with the approach",
                "Agreed",
                "Consensus reached"
        );

        ConsensusDetector.ConsensusResult result = detector.detectConsensus(messages, 3);

        assertThat(result.consensusReached()).isTrue();
    }

    @Test
    void lastMessageEmpty_noConsensus() {
        List<String> messages = List.of(
                "Some meaningful content here",
                "Another meaningful reply",
                "   "
        );

        ConsensusDetector.ConsensusResult result = detector.detectConsensus(messages, 3, 2);

        assertThat(result.consensusReached()).isFalse();
        assertThat(result.reason()).contains("no meaningful tokens");
    }

    private String reasonOrEmpty(ConsensusDetector.ConsensusResult result) {
        return result.reason() != null ? result.reason() : "";
    }
}
