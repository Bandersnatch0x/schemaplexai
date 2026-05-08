package com.schemaplexai.agent.engine.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Cryptographic hash utilities for data integrity verification.
 * <p>All comparison operations use constant-time algorithms to prevent timing attacks.
 */
public final class HashUtils {

    private HashUtils() {
        // utility class
    }

    /**
     * Computes the SHA-256 hash of the given input string.
     *
     * @param input the string to hash
     * @return the 64-character hexadecimal representation of the hash
     */
    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(64);
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Constant-time comparison of two strings to prevent timing attacks.
     *
     * @param a first string
     * @param b second string
     * @return true if the strings are equal
     */
    public static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return a == null && b == null;
        }
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8)
        );
    }
}
