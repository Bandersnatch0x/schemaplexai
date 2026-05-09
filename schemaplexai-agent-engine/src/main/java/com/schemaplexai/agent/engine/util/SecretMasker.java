package com.schemaplexai.agent.engine.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Masks sensitive data (secrets and PII) from JSON payloads and text.
 * <p>Secret key patterns are matched case-insensitively. PII patterns cover
 * email addresses and US phone numbers.
 * <p>All JSON operations return new nodes; input nodes are never mutated.
 */
public class SecretMasker {

    private static final String MASKED_SECRET = "***MASKED***";
    private static final String MASKED_PII = "***PII***";

    private static final List<String> SECRET_KEY_PATTERNS = List.of(
            "apikey",
            "api_key",
            "token",
            "secret",
            "password",
            "credential",
            "privatekey",
            "private_key"
    );

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b"
    );

    private static final Pattern US_PHONE_PATTERN = Pattern.compile(
            "\\b\\d{3}[-.]?\\d{3}[-.]?\\d{4}\\b"
    );

    private final ObjectMapper objectMapper;

    public SecretMasker(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Deep-traverses a JSON node and replaces values for keys matching secret
     * patterns with {@code "***MASKED***"}.
     *
     * @param input the JSON node to mask; may be {@code null}
     * @return a new JSON node with sensitive values masked, or {@code null} if input was null
     */
    public JsonNode maskJson(JsonNode input) {
        if (input == null) {
            return null;
        }
        return maskNode(input);
    }

    /**
     * Replaces email and US phone patterns in the given text with {@code "***PII***"}.
     *
     * @param text the text to mask; may be {@code null}
     * @return the masked text, or {@code null} if input was null
     */
    public String maskPii(String text) {
        if (text == null) {
            return null;
        }
        String result = EMAIL_PATTERN.matcher(text).replaceAll(MASKED_PII);
        return US_PHONE_PATTERN.matcher(result).replaceAll(MASKED_PII);
    }

    private JsonNode maskNode(JsonNode node) {
        if (node.isObject()) {
            return maskObject((ObjectNode) node);
        }
        if (node.isArray()) {
            return maskArray((ArrayNode) node);
        }
        return node.deepCopy();
    }

    private ObjectNode maskObject(ObjectNode objectNode) {
        ObjectNode result = objectMapper.createObjectNode();
        objectNode.fields().forEachRemaining(entry -> {
            String key = entry.getKey();
            JsonNode value = entry.getValue();
            if (isSecretKey(key) && value.isValueNode()) {
                result.set(key, new TextNode(MASKED_SECRET));
            } else {
                result.set(key, maskNode(value));
            }
        });
        return result;
    }

    private ArrayNode maskArray(ArrayNode arrayNode) {
        ArrayNode result = objectMapper.createArrayNode();
        for (JsonNode element : arrayNode) {
            result.add(maskNode(element));
        }
        return result;
    }

    private boolean isSecretKey(String key) {
        if (key == null) {
            return false;
        }
        String normalized = key.toLowerCase().replace("_", "");
        return SECRET_KEY_PATTERNS.stream().anyMatch(normalized::contains);
    }
}
