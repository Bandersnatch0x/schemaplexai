package com.schemaplexai.workflow.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Computes a deterministic SHA-256 hash of workflow topology (node configuration).
 *
 * <p>Used to detect silent corruption when restoring workflow checkpoints.
 * If the template's topology has changed since a checkpoint was created,
 * the hash mismatch prevents the workflow from resuming with stale state.
 *
 * <p>The hash is computed over the normalized nodeConfigJson, so semantically
 * equivalent JSON with different whitespace produces the same hash.
 */
public final class TopologyHasher {

    private TopologyHasher() {}

    /**
     * Compute a SHA-256 hash of the given node configuration JSON.
     *
     * <p>The input is normalized (whitespace-trimmed) before hashing to ensure
     * deterministic results regardless of formatting.
     *
     * @param nodeConfigJson the workflow node configuration JSON
     * @return hex-encoded SHA-256 hash, or null if input is null/blank
     */
    public static String hash(String nodeConfigJson) {
        if (nodeConfigJson == null || nodeConfigJson.isBlank()) {
            return null;
        }
        String normalized = normalize(nodeConfigJson);
        return sha256Hex(normalized);
    }

    /**
     * Verify that a checkpoint's topology hash matches the current template.
     *
     * @param expectedHash the hash stored in the checkpoint
     * @param currentJson  the current template node config JSON
     * @return true if the hashes match, or if both are null/blank
     * @throws TopologyMismatchException if hashes do not match
     */
    public static void verify(String expectedHash, String currentJson) {
        String currentHash = hash(currentJson);

        if (expectedHash == null && currentHash == null) {
            return;
        }
        if (expectedHash == null || currentHash == null) {
            throw new TopologyMismatchException(
                    "Topology mismatch: checkpoint hash=" + expectedHash
                            + ", current hash=" + currentHash);
        }
        if (!expectedHash.equals(currentHash)) {
            throw new TopologyMismatchException(
                    "Topology mismatch: checkpoint hash=" + expectedHash
                            + ", current hash=" + currentHash
                            + ". The workflow template has changed since this checkpoint was created.");
        }
    }

    /**
     * Normalize JSON for deterministic hashing.
     * Strips all whitespace outside of string literals.
     */
    static String normalize(String json) {
        StringBuilder sb = new StringBuilder(json.length());
        boolean inString = false;
        boolean escape = false;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escape) {
                sb.append(c);
                escape = false;
                continue;
            }
            if (c == '\\' && inString) {
                sb.append(c);
                escape = true;
                continue;
            }
            if (c == '"') {
                sb.append(c);
                inString = !inString;
                continue;
            }
            if (!inString && Character.isWhitespace(c)) {
                continue;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
