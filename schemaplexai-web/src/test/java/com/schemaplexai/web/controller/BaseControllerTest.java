package com.schemaplexai.web.controller;

import com.schemaplexai.common.result.Result;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BaseControllerTest {

    private static class TestController extends BaseController {}

    private final TestController controller = new TestController();

    @Test
    void successWithData() {
        Result<String> result = controller.success("hello");
        assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).isEqualTo("hello");
    }

    @Test
    void successWithoutData() {
        Result<Void> result = controller.success();
        assertThat(result.getCode()).isEqualTo(200);
    }

    @Test
    void errorWithMessage() {
        Result<Void> result = controller.error("fail");
        assertThat(result.getCode()).isEqualTo(500);
        assertThat(result.getMessage()).isEqualTo("fail");
    }

    @Test
    void errorWithCodeAndMessage() {
        Result<Void> result = controller.error(400, "bad request");
        assertThat(result.getCode()).isEqualTo(400);
        assertThat(result.getMessage()).isEqualTo("bad request");
    }
}
