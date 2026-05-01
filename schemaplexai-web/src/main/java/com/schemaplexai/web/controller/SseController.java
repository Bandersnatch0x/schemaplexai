package com.schemaplexai.web.controller;

import com.schemaplexai.common.result.Result;
import com.schemaplexai.web.sse.AgentSseEmitter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/sse")
@RequiredArgsConstructor
public class SseController extends BaseController {

    private final AgentSseEmitter agentSseEmitter;

    @GetMapping("/subscribe/{clientId}")
    public SseEmitter subscribe(@PathVariable String clientId,
                                @RequestHeader(value = "Authorization", required = false) String token) {
        return agentSseEmitter.createEmitter(clientId, token);
    }

    @PostMapping("/send/{clientId}")
    @PreAuthorize("hasAuthority('sse:admin:send')")
    public Result<Void> sendEvent(@PathVariable String clientId, @RequestParam String event, @RequestParam String data) {
        agentSseEmitter.sendEvent(clientId, event, data);
        return Result.success();
    }
}
