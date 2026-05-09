package com.schemaplexai.agent.engine.groupchat;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Detects consensus (agreement) among agents in a group chat.
 *
 * <p>Consensus is determined by keyword overlap between consecutive messages.
 * A configurable threshold controls how many matching keywords are required.
 * Additionally, explicit agreement phrases ("agree", "consensus", "LGTM", etc.)
 * are detected to fast-track consensus.
 */
@Slf4j
@Component
public class ConsensusDetector {

    private static final Set<String> AGREEMENT_PHRASES = Set.of(
            "agree", "agreed", "consensus", "lgtm", "looks good",
            "approved", "approve", "+1", "concur", "accepted"
    );

    private static final Set<String> STOP_WORDS = Set.of(
            "the", "a", "an", "is", "are", "was", "were", "be", "been",
            "being", "have", "has", "had", "do", "does", "did", "will",
            "would", "could", "should", "may", "might", "must", "shall",
            "can", "need", "dare", "ought", "used", "to", "of", "in",
            "for", "on", "with", "at", "by", "from", "as", "into",
            "through", "during", "before", "after", "above", "below",
            "between", "under", "and", "but", "or", "yet", "so", "if",
            "because", "although", "though", "while", "where", "when",
            "that", "which", "who", "whom", "whose", "what", "this",
            "these", "those", "i", "you", "he", "she", "it", "we", "they",
            "me", "him", "her", "us", "them", "my", "your", "his",
            "its", "our", "their", "mine", "yours", "hers", "ours", "theirs"
    );

    /**
     * Result of a consensus check.
     *
     * @param consensusReached true if consensus is detected
     * @param agreementScore   normalized score [0.0, 1.0] representing agreement strength
     * @param reason           human-readable explanation
     */
    public record ConsensusResult(boolean consensusReached, double agreementScore, String reason) {}

    /**
     * Checks whether the last N messages indicate consensus.
     *
     * @param messages          conversation history (most recent last)
     * @param requiredAgents    number of distinct agents that must agree
     * @param keywordThreshold  minimum matching keywords to count as agreement
     * @return consensus result with score and explanation
     */
    public ConsensusResult detectConsensus(List<String> messages, int requiredAgents, int keywordThreshold) {
        if (messages == null || messages.size() < requiredAgents) {
            return new ConsensusResult(false, 0.0,
                    "Insufficient messages (" + (messages == null ? 0 : messages.size()) + ") for consensus");
        }

        if (requiredAgents < 2) {
            return new ConsensusResult(true, 1.0, "Single-agent consensus trivially satisfied");
        }

        // Check last N messages for explicit agreement
        int checkCount = Math.min(requiredAgents, messages.size());
        List<String> recent = messages.subList(messages.size() - checkCount, messages.size());

        int explicitAgreements = 0;
        for (String msg : recent) {
            if (containsExplicitAgreement(msg)) {
                explicitAgreements++;
            }
        }

        if (explicitAgreements >= requiredAgents) {
            double score = (double) explicitAgreements / checkCount;
            return new ConsensusResult(true, score,
                    "Explicit agreement detected in " + explicitAgreements + " of " + checkCount + " messages");
        }

        // Keyword overlap between the last message and all previous recent messages
        String lastMessage = recent.get(recent.size() - 1);
        Set<String> lastTokens = tokenize(lastMessage);

        if (lastTokens.isEmpty()) {
            return new ConsensusResult(false, 0.0, "Last message contains no meaningful tokens");
        }

        int agreements = 0;
        double totalScore = 0.0;
        for (int i = 0; i < recent.size() - 1; i++) {
            Set<String> prevTokens = tokenize(recent.get(i));
            int overlap = countOverlap(lastTokens, prevTokens);
            if (overlap >= keywordThreshold) {
                agreements++;
            }
            totalScore += Math.min(1.0, (double) overlap / keywordThreshold);
        }

        double agreementScore = recent.size() > 1 ? totalScore / (recent.size() - 1) : 0.0;

        if (agreements >= requiredAgents - 1) {
            return new ConsensusResult(true, agreementScore,
                    "Keyword overlap consensus: " + agreements + " agreements with threshold " + keywordThreshold);
        }

        return new ConsensusResult(false, agreementScore,
                "No consensus: " + agreements + " agreements, need " + (requiredAgents - 1)
                        + ", threshold=" + keywordThreshold);
    }

    /**
     * Convenience overload with default threshold of 2 keywords.
     */
    public ConsensusResult detectConsensus(List<String> messages, int requiredAgents) {
        return detectConsensus(messages, requiredAgents, 2);
    }

    private boolean containsExplicitAgreement(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        String lower = message.toLowerCase(Locale.ROOT);
        for (String phrase : AGREEMENT_PHRASES) {
            if (lower.contains(phrase)) {
                return true;
            }
        }
        return false;
    }

    private Set<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }
        String[] rawTokens = text.toLowerCase(Locale.ROOT).split("\\W+");
        java.util.Set<String> unique = new java.util.HashSet<>();
        for (String token : rawTokens) {
            if (token.length() > 2 && !STOP_WORDS.contains(token)) {
                unique.add(token);
            }
        }
        return Set.copyOf(unique);
    }

    private int countOverlap(Set<String> a, Set<String> b) {
        int count = 0;
        for (String token : a) {
            if (b.contains(token)) {
                count++;
            }
        }
        return count;
    }
}
