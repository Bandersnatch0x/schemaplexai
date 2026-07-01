package com.schemaplexai.web.controller;

import com.schemaplexai.common.controller.BaseController;

import com.schemaplexai.common.result.Result;
import com.schemaplexai.web.dto.SseEvent;
import com.schemaplexai.web.service.SseReplayService;
import com.schemaplexai.web.sse.AgentSseEmitter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/sse")
@RequiredArgsConstructor
@Tag(name = "SSE消息推送", description = "Server-Sent Events订阅和推送")
public class SseController extends BaseController {

    private final AgentSseEmitter agentSseEmitter;
    private final SseReplayService sseReplayService;

    @Operation(summary = "订阅SSE事件流")
    @GetMapping("/subscribe/{clientId}")
    public SseEmitter subscribe(@PathVariable String clientId,
                                @RequestHeader(value = "Authorization", required = false) String token) {
        return agentSseEmitter.createEmitter(clientId, token);
    }

    /**
     * Subscribe to execution-specific events with replay support.
     * Replays events with seq > lastSeq (excluding EPHEMERAL), then opens live SSE stream.
     */
    @Operation(summary = "订阅执行事件流（带回放）")
    @GetMapping("/executions/{executionId}/events")
    public SseEmitter subscribeExecutionEvents(
            @PathVariable Long executionId,
            @RequestParam(required = false, defaultValue = "0") int lastSeq,
            @RequestHeader(value = "Authorization", required = false) String token) {

        String clientId = "exec:" + executionId + ":" + System.currentTimeMillis();
        SseEmitter emitter = agentSseEmitter.subscribeExecution(clientId, executionId, token);

        // Replay missed events
        List<SseEvent> replayEvents = sseReplayService.replayEvents(executionId, lastSeq);
        for (SseEvent event : replayEvents) {
            agentSseEmitter.sendEvent(clientId, event.eventType(), event);
        }

        return emitter;
    }

    @Operation(summary = "向指定客户端发送SSE事件")
    @PostMapping("/send/{clientId}")
    @PreAuthorize("hasAuthority('sse:admin:send')")
    public Result<Void> sendEvent(@PathVariable String clientId, @RequestParam String event, @RequestParam String data) {
        agentSseEmitter.sendEvent(clientId, event, data);
        return Result.success();
    }
}
