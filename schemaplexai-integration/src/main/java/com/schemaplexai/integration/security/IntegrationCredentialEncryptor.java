package com.schemaplexai.integration.security;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
 * AES-256-GCM credential encryption for the integration layer with tenant-scoped keys.
 *
 * <p>Self-contained equivalent of the agent-engine {@code TenantKeyService} pattern,
 * deliberately duplicated inside this module to avoid a cross-module dependency cycle.
 * Keys are derived from a master secret + tenant ID using PBKDF2-HmacSHA256, so each
 * tenant holds an isolated AES-256 key and can never decrypt another tenant's credentials.
 *
 * <p>The master secret is sourced from configuration property
 * {@code integration.encryption.master-secret}, which is bound to the
 * {@code INTEGRATION_MASTER_SECRET} environment variable (see application.yml).
 *
 * <p>Ciphertext format: {@value #CIPHER_PREFIX} + Base64(IV[12] || AES-GCM-ciphertext+tag).
 */
@Slf4j
@Component
public class IntegrationCredentialEncryptor {

    public static final String CIPHER_PREFIX = "enc:v1:";

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;
    private static final int AES_KEY_LENGTH = 256;
    private static final int PBKDF2_ITERATIONS = 100_000;
    private static final String KEY_SALT_PREFIX = "schemaplexai:integration:";

    /** Cache derived keys to avoid repeated PBKDF2 computation. */
    private final ConcurrentHashMap<Long, SecretKey> keyCache = new ConcurrentHashMap<>();
    private final String masterSecret;

    @Autowired
    public IntegrationCredentialEncryptor(
            @Value("${integration.encryption.master-secret:}") String masterSecret) {
        // Review ST-04: fail fast instead of falling back to a public dev literal.
        // The master key protects Git access tokens / OAuth tokens (AES-256-GCM,
        // tenant-derived keys); a fallback key shared by every deployment would make
        // the tenant key isolation meaningless. Mirrors the JwtSecretStartupValidator
        // contract: missing secret => startup failure.
        if (masterSecret == null || masterSecret.isBlank()) {
            throw new IllegalStateException(
                    "Integration master secret is not configured. "
                            + "Set the INTEGRATION_MASTER_SECRET environment variable or the "
                            + "integration.encryption.master-secret property.");
        }
        this.masterSecret = masterSecret;
    }

    /**
     * Encrypt a credential with the tenant-scoped AES-256-GCM key and a fresh random IV.
     *
     * @return prefixed ciphertext: {@code enc:v1:Base64(IV || ciphertext || tag)}
     */
    public String encrypt(String plaintext, Long tenantId) {
        requireTenant(tenantId);
        if (plaintext == null) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Plaintext credential must not be null");
        }
        try {
            SecretKey key = getKey(tenantId);
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

            return CIPHER_PREFIX + Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new BaseException(ResultCode.ERROR, "Credential encryption failed for tenant: " + tenantId, e);
        }
    }

    /**
     * Decrypt a prefixed ciphertext produced by {@link #encrypt(String, Long)}.
     * Decryption is only possible with the same tenant key that encrypted the value.
     */
    public String decrypt(String ciphertext, Long tenantId) {
        requireTenant(tenantId);
        if (ciphertext == null || !ciphertext.startsWith(CIPHER_PREFIX)) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Value is not a recognized encrypted credential");
        }
        try {
            byte[] combined = Base64.getDecoder().decode(ciphertext.substring(CIPHER_PREFIX.length()));
            if (combined.length <= GCM_IV_LENGTH) {
                throw new IllegalArgumentException("Ciphertext too short");
            }
            byte[] iv = Arrays.copyOfRange(combined, 0, GCM_IV_LENGTH);
            byte[] encrypted = Arrays.copyOfRange(combined, GCM_IV_LENGTH, combined.length);

            SecretKey key = getKey(tenantId);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new BaseException(ResultCode.ERROR, "Credential decryption failed for tenant: " + tenantId, e);
        }
    }

    /** Returns true when the stored value carries the encryption prefix. */
    public static boolean isEncrypted(String storedValue) {
        return storedValue != null && storedValue.startsWith(CIPHER_PREFIX);
    }

    private void requireTenant(Long tenantId) {
        if (tenantId == null) {
            throw new BaseException(ResultCode.PARAM_ERROR, "Tenant ID is required for credential encryption");
        }
    }

    private SecretKey getKey(Long tenantId) {
        return keyCache.computeIfAbsent(tenantId, this::deriveKey);
    }

    /**
     * Derive an AES-256 key from master secret + tenant ID using PBKDF2.
     * The tenant ID acts as salt, so every tenant gets an isolated key.
     */
    private SecretKey deriveKey(Long tenantId) {
        try {
            byte[] salt = (KEY_SALT_PREFIX + tenantId).getBytes(StandardCharsets.UTF_8);
            KeySpec spec = new PBEKeySpec(masterSecret.toCharArray(), salt, PBKDF2_ITERATIONS, AES_KEY_LENGTH);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] keyBytes = factory.generateSecret(spec).getEncoded();
            return new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            throw new BaseException(ResultCode.ERROR, "Key derivation failed for tenant: " + tenantId, e);
        }
    }
}
