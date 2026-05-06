package com.schemaplexai.common.result;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResultCodeTest {

    @Test
    void success_hasCode200AndMessage() {
        assertThat(ResultCode.SUCCESS.getCode()).isEqualTo(200);
        assertThat(ResultCode.SUCCESS.getMessage()).isEqualTo("success");
    }

    @Test
    void error_hasCode500AndMessage() {
        assertThat(ResultCode.ERROR.getCode()).isEqualTo(500);
        assertThat(ResultCode.ERROR.getMessage()).isEqualTo("system error");
    }

    @Test
    void paramError_hasCode400() {
        assertThat(ResultCode.PARAM_ERROR.getCode()).isEqualTo(400);
        assertThat(ResultCode.PARAM_ERROR.getMessage()).isEqualTo("param error");
    }

    @Test
    void unauthorized_hasCode401() {
        assertThat(ResultCode.UNAUTHORIZED.getCode()).isEqualTo(401);
        assertThat(ResultCode.UNAUTHORIZED.getMessage()).isEqualTo("unauthorized");
    }

    @Test
    void forbidden_hasCode403() {
        assertThat(ResultCode.FORBIDDEN.getCode()).isEqualTo(403);
        assertThat(ResultCode.FORBIDDEN.getMessage()).isEqualTo("forbidden");
    }

    @Test
    void notFound_hasCode404() {
        assertThat(ResultCode.NOT_FOUND.getCode()).isEqualTo(404);
        assertThat(ResultCode.NOT_FOUND.getMessage()).isEqualTo("not found");
    }

    @Test
    void tenantCodes_haveCorrectValues() {
        assertThat(ResultCode.TENANT_NOT_FOUND.getCode()).isEqualTo(1001);
        assertThat(ResultCode.TENANT_DISABLED.getCode()).isEqualTo(1002);
    }

    @Test
    void authCodes_haveCorrectValues() {
        assertThat(ResultCode.TOKEN_EXPIRED.getCode()).isEqualTo(2001);
        assertThat(ResultCode.TOKEN_INVALID.getCode()).isEqualTo(2002);
        assertThat(ResultCode.USER_NOT_FOUND.getCode()).isEqualTo(2003);
        assertThat(ResultCode.PASSWORD_ERROR.getCode()).isEqualTo(2004);
    }

    @Test
    void agentCodes_haveCorrectValues() {
        assertThat(ResultCode.AGENT_NOT_FOUND.getCode()).isEqualTo(3001);
        assertThat(ResultCode.AGENT_EXECUTION_FAILED.getCode()).isEqualTo(3002);
        assertThat(ResultCode.AGENT_RATE_LIMIT.getCode()).isEqualTo(3003);
        assertThat(ResultCode.TOKEN_BUDGET_EXCEEDED.getCode()).isEqualTo(3004);
        assertThat(ResultCode.LOOP_DETECTED.getCode()).isEqualTo(3005);
    }

    @Test
    void workflowCodes_haveCorrectValues() {
        assertThat(ResultCode.WORKFLOW_NOT_FOUND.getCode()).isEqualTo(4001);
        assertThat(ResultCode.WORKFLOW_INSTANCE_NOT_FOUND.getCode()).isEqualTo(4002);
    }

    @Test
    void allEnumValues_areAccessible() {
        ResultCode[] values = ResultCode.values();
        assertThat(values).isNotEmpty();
        assertThat(values.length).isGreaterThanOrEqualTo(20);
    }
}
