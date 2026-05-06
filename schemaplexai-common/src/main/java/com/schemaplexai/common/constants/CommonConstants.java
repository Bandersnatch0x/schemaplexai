package com.schemaplexai.common.constants;

public final class CommonConstants {

    private CommonConstants() {}

    // Tenant
    public static final String HEADER_TENANT_ID = "X-Tenant-Id";
    public static final String CONTEXT_TENANT_ID = "tenantId";

    // User
    public static final String HEADER_USER_ID = "X-User-Id";

    // JWT
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";

    // Redis Keys
    public static final String REDIS_KEY_CHAT_MEMORY = "sf:memory:chat:%s";
    public static final String REDIS_KEY_TEAM_CONTEXT = "sf:context:team:%s";
    public static final String REDIS_KEY_IDEMPOTENCY = "sf:idempotency:%s";
    public static final String REDIS_KEY_RATE_LIMIT = "sf:rate:%s:%s";
    public static final String REDIS_KEY_EXECUTION_PAUSED = "sf:execution:paused:%s";
    public static final String REDIS_KEY_MODEL_COOLDOWN = "sf:model:cooldown:%s";

    // MQ Exchange & Routing Keys
    public static final String EXCHANGE_SCHEMAPLEXAI = "sf.exchange";
    public static final String RK_AGENT_EXECUTE = "sf.agent.execute";
    public static final String RK_AGENT_EXEC_EVENT = "sf.agent.exec.event";
    public static final String RK_AGENT_TEAM_CONTEXT = "sf.agent.team.context";
    public static final String RK_WORKFLOW_TRIGGER = "sf.workflow.trigger";
    public static final String RK_NOTIFICATION = "sf.notification";
    public static final String RK_COST = "sf.cost";
    public static final String RK_QUALITY = "sf.quality";
    public static final String RK_MILVUS_SYNC = "sf.milvus.sync";
    public static final String RK_AGENT_CONFIG_SHADOW = "sf.agent.config.shadow";

    // Default values
    public static final Long DEFAULT_MAX_ROUNDS = 20L;
    public static final Long DEFAULT_MAX_TOOLS = 10L;
    public static final Long DEFAULT_MAX_INPUT_TOKENS = 32000L;
    public static final Long DEFAULT_MAX_OUTPUT_TOKENS = 4096L;
}
