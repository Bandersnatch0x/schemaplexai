package com.schemaplexai.integration.security;

import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IntegrationCredentialEncryptorTest {

    private static final String MASTER_SECRET = "unit-test-master-secret";

    private IntegrationCredentialEncryptor encryptor;

    @BeforeEach
    void setUp() {
        encryptor = new IntegrationCredentialEncryptor(MASTER_SECRET);
    }

    @Test
    void encryptDecrypt_roundTrip_returnsOriginal() {
        String token = "ghp_abcdef123456";
        String cipher = encryptor.encrypt(token, 1L);
        assertThat(encryptor.decrypt(cipher, 1L)).isEqualTo(token);
    }

    @Test
    void encrypt_producesVersionedCiphertext_notPlaintext() {
        String token = "super-secret-token";
        String cipher = encryptor.encrypt(token, 1L);

        assertThat(cipher).startsWith(IntegrationCredentialEncryptor.CIPHER_PREFIX);
        assertThat(cipher).doesNotContain(token);
        assertThat(IntegrationCredentialEncryptor.isEncrypted(cipher)).isTrue();
        assertThat(IntegrationCredentialEncryptor.isEncrypted(token)).isFalse();
        assertThat(IntegrationCredentialEncryptor.isEncrypted(null)).isFalse();
    }

    @Test
    void encrypt_isNonDeterministic_dueToRandomIv() {
        String token = "same-token";
        String first = encryptor.encrypt(token, 1L);
        String second = encryptor.encrypt(token, 1L);
        assertThat(first).isNotEqualTo(second);
        assertThat(encryptor.decrypt(first, 1L)).isEqualTo(token);
        assertThat(encryptor.decrypt(second, 1L)).isEqualTo(token);
    }

    @Test
    void decrypt_withDifferentTenantKey_fails() {
        String cipher = encryptor.encrypt("tenant-1-secret", 1L);
        assertThatThrownBy(() -> encryptor.decrypt(cipher, 2L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.ERROR.getCode());
    }

    @Test
    void decrypt_tamperedCiphertext_fails() {
        String cipher = encryptor.encrypt("secret", 1L);
        // Flip a byte in the middle of the decoded payload to simulate tampering
        String body = cipher.substring(IntegrationCredentialEncryptor.CIPHER_PREFIX.length());
        byte[] decoded = java.util.Base64.getDecoder().decode(body);
        decoded[decoded.length / 2] ^= 0xFF;
        String tampered = IntegrationCredentialEncryptor.CIPHER_PREFIX
                + java.util.Base64.getEncoder().encodeToString(decoded);

        assertThatThrownBy(() -> encryptor.decrypt(tampered, 1L))
                .isInstanceOf(BaseException.class);
    }

    @Test
    void decrypt_unprefixedValue_rejectedAsNotEncrypted() {
        assertThatThrownBy(() -> encryptor.decrypt("plain-token", 1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void decrypt_nullCiphertext_throwsParamError() {
        assertThatThrownBy(() -> encryptor.decrypt(null, 1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void encrypt_nullTenant_throwsParamError() {
        assertThatThrownBy(() -> encryptor.encrypt("secret", null))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void decrypt_nullTenant_throwsParamError() {
        String cipher = encryptor.encrypt("secret", 1L);
        assertThatThrownBy(() -> encryptor.decrypt(cipher, null))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void encrypt_nullPlaintext_throwsParamError() {
        assertThatThrownBy(() -> encryptor.encrypt(null, 1L))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.PARAM_ERROR.getCode());
    }

    @Test
    void blankMasterSecret_failsFastAtStartup() {
        assertThatThrownBy(() -> new IntegrationCredentialEncryptor(""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("INTEGRATION_MASTER_SECRET");
        assertThatThrownBy(() -> new IntegrationCredentialEncryptor(null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void differentMasterSecret_cannotDecrypt() {
        String cipher = encryptor.encrypt("secret", 1L);
        IntegrationCredentialEncryptor other = new IntegrationCredentialEncryptor("another-master-secret");
        assertThatThrownBy(() -> other.decrypt(cipher, 1L))
                .isInstanceOf(BaseException.class);
    }
}
