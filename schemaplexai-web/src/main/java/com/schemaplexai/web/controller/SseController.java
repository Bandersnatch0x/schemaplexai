package com.schemaplexai.web.controller;

import com.schemaplexai.common.result.Result;
import com.schemaplexai.web.sse.AgentSseEmitter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/sse")
@RequiredArgsConstructor
@Tag(name = "SSE消息推送", description = "Server-Sent Events订阅和推送")
public class SseController extends BaseController {

    private final AgentSseEmitter agentSseEmitter;

    @Operation(summary = "订阅SSE事件流")
    @GetMapping("/subscribe/{clientId}")
    public SseEmitter subscribe(@PathVariable String clientId,
                                @RequestHeader(value = "Authorization", required = false) String token) {
        return agentSseEmitter.createEmitter(clientId, token);
    }

    @Operation(summary = "向指定客户端发送SSE事件")
    @PostMapping("/send/{clientId}")
    @PreAuthorize("hasAuthority('sse:admin:send')")
    public Result<Void> sendEvent(@PathVariable String clientId, @RequestParam String event, @RequestParam String data) {
        agentSseEmitter.sendEvent(clientId, event, data);
        return Result.success();
    }
}
