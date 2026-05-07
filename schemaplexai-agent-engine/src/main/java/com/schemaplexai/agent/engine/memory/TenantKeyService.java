package com.schemaplexai.agent.engine.memory;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.schemaplexai.common.result.ResultCode;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-tenant AES-256 key management for ChatMemory encryption.
 *
 * Keys are derived from a master secret + tenant ID using PBKDF2.
 * Each tenant gets an isolated encryption key.
 * Ciphertext format: Base64(IV[12] || AES-GCM-ciphertext[tag+data])
 */
@Slf4j
@Component
public class TenantKeyService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;
    private static final int AES_KEY_LENGTH = 256;
    private static final int PBKDF2_ITERATIONS = 100_000;

    /** Cache derived keys to avoid repeated PBKDF2 computation. */
    private final ConcurrentHashMap<String, SecretKey> keyCache = new ConcurrentHashMap<>();
    private final String masterSecret;

    public TenantKeyService(@Value("${chat.memory.encryption.master-secret:}") String masterSecret) {
        if (masterSecret == null || masterSecret.isBlank()) {
            log.warn("No master secret configured — using fallback (DEV ONLY). Set chat.memory.encryption.master-secret in production.");
            this.masterSecret = "dev-only-fallback-secret-do-not-use-in-prod!";
        } else {
            this.masterSecret = masterSecret;
        }
    }

    /**
     * Constructor for unit tests that don't have Spring context.
     */
    public TenantKeyService(String masterSecret) {
        this.masterSecret = masterSecret;
    }

    /**
     * Get or derive the AES-256 key for a tenant.
     */
    public SecretKey getKey(String tenantId) {
        return keyCache.computeIfAbsent(tenantId, this::deriveKey);
    }

    /**
     * Encrypt plaintext using AES-256-GCM with a random IV.
     * Returns Base64(IV || ciphertext || tag).
     */
    public String encrypt(String plaintext, String tenantId) {
        try {
            SecretKey key = getKey(tenantId);
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom.getInstanceStrong().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // Prepend IV to ciphertext
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new com.schemaplexai.common.exception.BaseException(
                    ResultCode.ERROR, "Encryption failed for tenant: " + tenantId, e);
        }
    }

    /**
     * Decrypt Base64(IV || ciphertext || tag) using AES-256-GCM.
     */
    public String decrypt(String ciphertext, String tenantId) {
        try {
            byte[] combined = Base64.getDecoder().decode(ciphertext);
            byte[] iv = Arrays.copyOfRange(combined, 0, GCM_IV_LENGTH);
            byte[] encrypted = Arrays.copyOfRange(combined, GCM_IV_LENGTH, combined.length);

            SecretKey key = getKey(tenantId);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new com.schemaplexai.common.exception.BaseException(
                    ResultCode.ERROR, "Decryption failed for tenant: " + tenantId, e);
        }
    }

    /**
     * Derive an AES-256 key from master secret + tenant ID using PBKDF2.
     * Salt is the tenant ID bytes (each tenant gets a unique key).
     */
    private SecretKey deriveKey(String tenantId) {
        try {
            // Use tenant ID as salt — deterministic per tenant
            byte[] salt = ("schemaplexai:" + tenantId).getBytes(StandardCharsets.UTF_8);
            KeySpec spec = new PBEKeySpec(masterSecret.toCharArray(), salt, PBKDF2_ITERATIONS, AES_KEY_LENGTH);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] keyBytes = factory.generateSecret(spec).getEncoded();
            return new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            throw new com.schemaplexai.common.exception.BaseException(
                    ResultCode.ERROR, "Key derivation failed for tenant: " + tenantId, e);
        }
    }
}
