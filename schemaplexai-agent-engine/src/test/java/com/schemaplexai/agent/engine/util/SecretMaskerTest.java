package com.schemaplexai.agent.engine.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("M6.2: Secret Masker Tests")
class SecretMaskerTest {

    private ObjectMapper objectMapper;
    private SecretMasker secretMasker;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        secretMasker = new SecretMasker(objectMapper);
    }

    @Test
    @DisplayName("masks apiKey field in JSON")
    void masksApiKeyField() {
        ObjectNode input = objectMapper.createObjectNode();
        input.put("apiKey", "sk-1234567890abcdef");
        input.put("name", "test-service");

        JsonNode result = secretMasker.maskJson(input);

        assertThat(result.get("apiKey").asText()).isEqualTo("***MASKED***");
        assertThat(result.get("name").asText()).isEqualTo("test-service");
    }

    @Test
    @DisplayName("masks nested secret fields")
    void masksNestedSecretFields() {
        ObjectNode credentials = objectMapper.createObjectNode();
        credentials.put("password", "superSecret123");
        credentials.put("username", "admin");

        ObjectNode input = objectMapper.createObjectNode();
        input.set("credentials", credentials);
        input.put("environment", "production");

        JsonNode result = secretMasker.maskJson(input);

        assertThat(result.get("credentials").get("password").asText()).isEqualTo("***MASKED***");
        assertThat(result.get("credentials").get("username").asText()).isEqualTo("admin");
        assertThat(result.get("environment").asText()).isEqualTo("production");
    }

    @Test
    @DisplayName("masks email in text")
    void masksEmailInText() {
        String text = "Contact us at support@example.com for help.";

        String result = secretMasker.maskPii(text);

        assertThat(result).isEqualTo("Contact us at ***PII*** for help.");
    }

    @Test
    @DisplayName("masks US phone in text")
    void masksUsPhoneInText() {
        String text = "Call 555-123-4567 or 555.987.6543 today.";

        String result = secretMasker.maskPii(text);

        assertThat(result).isEqualTo("Call ***PII*** or ***PII*** today.");
    }

    @Test
    @DisplayName("leaves non-sensitive fields unchanged")
    void leavesNonSensitiveFieldsUnchanged() {
        ObjectNode input = objectMapper.createObjectNode();
        input.put("id", 42);
        input.put("name", "test-name");
        input.put("enabled", true);
        input.put("count", 99);

        JsonNode result = secretMasker.maskJson(input);

        assertThat(result.get("id").asInt()).isEqualTo(42);
        assertThat(result.get("name").asText()).isEqualTo("test-name");
        assertThat(result.get("enabled").asBoolean()).isTrue();
        assertThat(result.get("count").asInt()).isEqualTo(99);
    }

    @Test
    @DisplayName("handles arrays with secret objects")
    void handlesArraysWithSecretObjects() {
        ObjectNode item1 = objectMapper.createObjectNode();
        item1.put("token", "abc123");
        item1.put("label", "first");

        ObjectNode item2 = objectMapper.createObjectNode();
        item2.put("secret", "xyz789");
        item2.put("label", "second");

        ArrayNode input = objectMapper.createArrayNode();
        input.add(item1);
        input.add(item2);

        JsonNode result = secretMasker.maskJson(input);

        assertThat(result.get(0).get("token").asText()).isEqualTo("***MASKED***");
        assertThat(result.get(0).get("label").asText()).isEqualTo("first");
        assertThat(result.get(1).get("secret").asText()).isEqualTo("***MASKED***");
        assertThat(result.get(1).get("label").asText()).isEqualTo("second");
    }

    @Test
    @DisplayName("masks all secret key patterns case-insensitively")
    void masksAllSecretPatternsCaseInsensitively() {
        ObjectNode input = objectMapper.createObjectNode();
        input.put("apiKey", "key1");
        input.put("API_KEY", "key2");
        input.put("token", "tok1");
        input.put("AUTH_TOKEN", "tok2");
        input.put("secret", "sec1");
        input.put("clientSecret", "sec2");
        input.put("password", "pwd1");
        input.put("userPassword", "pwd2");
        input.put("credential", "cred1");
        input.put("credentials", "cred2");
        input.put("privateKey", "pk1");
        input.put("myPrivateKey", "pk2");
        input.put("publicKey", "pub1");

        JsonNode result = secretMasker.maskJson(input);

        assertThat(result.get("apiKey").asText()).isEqualTo("***MASKED***");
        assertThat(result.get("API_KEY").asText()).isEqualTo("***MASKED***");
        assertThat(result.get("token").asText()).isEqualTo("***MASKED***");
        assertThat(result.get("AUTH_TOKEN").asText()).isEqualTo("***MASKED***");
        assertThat(result.get("secret").asText()).isEqualTo("***MASKED***");
        assertThat(result.get("clientSecret").asText()).isEqualTo("***MASKED***");
        assertThat(result.get("password").asText()).isEqualTo("***MASKED***");
        assertThat(result.get("userPassword").asText()).isEqualTo("***MASKED***");
        assertThat(result.get("credential").asText()).isEqualTo("***MASKED***");
        assertThat(result.get("credentials").asText()).isEqualTo("***MASKED***");
        assertThat(result.get("privateKey").asText()).isEqualTo("***MASKED***");
        assertThat(result.get("myPrivateKey").asText()).isEqualTo("***MASKED***");
        assertThat(result.get("publicKey").asText()).isEqualTo("pub1");
    }

    @Test
    @DisplayName("handles deeply nested objects and arrays")
    void handlesDeeplyNestedStructures() {
        ObjectNode deep = objectMapper.createObjectNode();
        deep.put("apiKey", "deep-key");

        ObjectNode middle = objectMapper.createObjectNode();
        middle.set("deep", deep);
        middle.put("token", "mid-tok");

        ArrayNode arr = objectMapper.createArrayNode();
        ObjectNode arrItem = objectMapper.createObjectNode();
        arrItem.put("password", "arr-pwd");
        arr.add(arrItem);
        middle.set("items", arr);

        ObjectNode input = objectMapper.createObjectNode();
        input.set("middle", middle);

        JsonNode result = secretMasker.maskJson(input);

        assertThat(result.get("middle").get("deep").get("apiKey").asText()).isEqualTo("***MASKED***");
        assertThat(result.get("middle").get("token").asText()).isEqualTo("***MASKED***");
        assertThat(result.get("middle").get("items").get(0).get("password").asText()).isEqualTo("***MASKED***");
    }

    @Test
    @DisplayName("returns null JSON node unchanged")
    void returnsNullJsonNodeUnchanged() {
        JsonNode result = secretMasker.maskJson(null);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("returns null text unchanged")
    void returnsNullTextUnchanged() {
        String result = secretMasker.maskPii(null);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("masks multiple emails and phones in same text")
    void masksMultiplePiiInSameText() {
        String text = "Emails: alice@example.com, bob@test.org. Phones: 8005551234, 800-555-5678.";

        String result = secretMasker.maskPii(text);

        assertThat(result).isEqualTo("Emails: ***PII***, ***PII***. Phones: ***PII***, ***PII***.");
    }
}
