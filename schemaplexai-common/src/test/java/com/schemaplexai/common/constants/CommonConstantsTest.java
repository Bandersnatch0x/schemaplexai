package com.schemaplexai.common.constants;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import static org.assertj.core.api.Assertions.assertThat;

class CommonConstantsTest {

    @Test
    void headerTenantId_hasExpectedValue() {
        assertThat(CommonConstants.HEADER_TENANT_ID).isEqualTo("X-Tenant-Id");
    }

    @Test
    void headerAuthorization_hasExpectedValue() {
        assertThat(CommonConstants.HEADER_AUTHORIZATION).isEqualTo("Authorization");
    }

    @Test
    void tokenPrefix_hasExpectedValue() {
        assertThat(CommonConstants.TOKEN_PREFIX).isEqualTo("Bearer ");
    }

    @Test
    void exchangeName_hasExpectedValue() {
        assertThat(CommonConstants.EXCHANGE_SCHEMAPLEXAI).isEqualTo("sf.exchange");
    }

    @Test
    void defaultMaxRounds_hasExpectedValue() {
        assertThat(CommonConstants.DEFAULT_MAX_ROUNDS).isEqualTo(20L);
    }

    @Test
    void defaultMaxTools_hasExpectedValue() {
        assertThat(CommonConstants.DEFAULT_MAX_TOOLS).isEqualTo(10L);
    }

    @Test
    void defaultMaxInputTokens_hasExpectedValue() {
        assertThat(CommonConstants.DEFAULT_MAX_INPUT_TOKENS).isEqualTo(32000L);
    }

    @Test
    void defaultMaxOutputTokens_hasExpectedValue() {
        assertThat(CommonConstants.DEFAULT_MAX_OUTPUT_TOKENS).isEqualTo(4096L);
    }

    @Test
    void redisKeyChatMemory_hasPlaceholder() {
        assertThat(CommonConstants.REDIS_KEY_CHAT_MEMORY).contains("%s");
    }

    @Test
    void routingKeys_startWithSfPrefix() {
        assertThat(CommonConstants.RK_AGENT_EXECUTE).startsWith("sf.");
        assertThat(CommonConstants.RK_AGENT_EXEC_EVENT).startsWith("sf.");
        assertThat(CommonConstants.RK_WORKFLOW_TRIGGER).startsWith("sf.");
        assertThat(CommonConstants.RK_NOTIFICATION).startsWith("sf.");
    }

    @Test
    void constructor_isPrivate() throws NoSuchMethodException {
        Constructor<CommonConstants> constructor = CommonConstants.class.getDeclaredConstructor();
        assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();
    }
}
