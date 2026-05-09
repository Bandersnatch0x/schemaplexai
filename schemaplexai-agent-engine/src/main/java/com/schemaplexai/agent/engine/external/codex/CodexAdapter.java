package com.schemaplexai.agent.engine.external.codex;

import com.schemaplexai.agent.engine.external.AgentEvent;
import com.schemaplexai.agent.engine.external.ExternalAgentAdapter;
import com.schemaplexai.agent.engine.external.ExternalAgentConfig;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Reference implementation of {@link ExternalAgentAdapter} for OpenAI Codex CLI.
 *
 * <p>Spawns the {@code codex} command-line tool via {@link ProcessBuilder} for each
 * session, capturing stdout/stderr as {@link AgentEvent} instances.
 *
 * <p>Requires the {@code codex} CLI to be installed and available on the system PATH.
 * Configuration is read from {@link ExternalAgentConfig}.
 */
@Slf4j
@Component
public class CodexAdapter implements ExternalAgentAdapter {

    private static final int MAX_OUTPUT_BYTES = 1 << 20; // 1 MiB
    private static final String DEFAULT_MODEL = "gpt-4o";

    private final ExternalAgentConfig config;
    private final Map<String, SessionContext> sessions = new ConcurrentHashMap<>();

    public CodexAdapter(ExternalAgentConfig config) {
        this.config = config;
    }

    @Override
    public void startSession(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "sessionId must not be null or blank");
        }
        if (sessions.containsKey(sessionId)) {
            log.warn("Codex session {} already exists; reusing existing session", sessionId);
            return;
        }

        Path workspace;
        try {
            workspace = Files.createTempDirectory("codex-" + sessionId + "-");
        } catch (IOException e) {
            log.error("Failed to create workspace for session {}", sessionId, e);
            throw new BaseException(ResultCode.AGENT_EXECUTION_FAILED,
                    "Failed to create Codex workspace: " + e.getMessage(), e);
        }

        sessions.put(sessionId, new SessionContext(sessionId, workspace));
        log.info("Codex session {} started, workspace={}", sessionId, workspace);
    }

    @Override
    public AgentEvent sendMessage(String sessionId, String message) {
        SessionContext ctx = requireSession(sessionId);
        if (message == null || message.isBlank()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "message must not be null or blank");
        }

        validateConfiguration();

        List<String> command = buildCodexCommand(ctx, message);
        log.debug("Codex session {} executing: {}", sessionId, command);

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(ctx.workspace.toFile());
        pb.redirectErrorStream(true);

        StringBuilder output = new StringBuilder();
        int exitCode;
        long startMs = System.currentTimeMillis();

        try {
            Process process = pb.start();
            boolean finished = process.waitFor(config.getTimeoutMs(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                log.warn("Codex session {} command timed out after {}ms", sessionId, config.getTimeoutMs());
                throw new BaseException(ResultCode.REQUEST_TIMEOUT,
                        "Codex command timed out after " + config.getTimeoutMs() + "ms");
            }
            exitCode = process.exitValue();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (output.length() + line.length() > MAX_OUTPUT_BYTES) {
                        output.append("\n[...truncated...]");
                        break;
                    }
                    output.append(line).append("\n");
                }
            }
        } catch (IOException e) {
            log.error("Codex session {} I/O error", sessionId, e);
            throw new BaseException(ResultCode.AGENT_EXECUTION_FAILED,
                    "Codex I/O error: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Codex session {} interrupted", sessionId, e);
            throw new BaseException(ResultCode.AGENT_EXECUTION_FAILED,
                    "Codex execution interrupted: " + e.getMessage(), e);
        }

        long latencyMs = System.currentTimeMillis() - startMs;
        String type = exitCode == 0 ? "response" : "error";
        AgentEvent event = new AgentEvent(type, output.toString().trim());
        ctx.events.add(event);

        log.debug("Codex session {} completed in {}ms, exitCode={}, type={}",
                sessionId, latencyMs, exitCode, type);
        return event;
    }

    @Override
    public List<AgentEvent> getEvents(String sessionId) {
        if (sessionId == null) {
            return Collections.emptyList();
        }
        SessionContext ctx = sessions.get(sessionId);
        if (ctx == null) {
            return Collections.emptyList();
        }
        return List.copyOf(ctx.events);
    }

    @Override
    public void terminateSession(String sessionId) {
        SessionContext ctx = sessions.remove(sessionId);
        if (ctx == null) {
            log.warn("Codex session {} not found for termination", sessionId);
            return;
        }
        try {
            deleteRecursively(ctx.workspace);
            log.info("Codex session {} terminated, workspace cleaned", sessionId);
        } catch (IOException e) {
            log.warn("Failed to clean workspace for session {}: {}", sessionId, e.getMessage());
        }
    }

    // --- Private helpers ---

    private SessionContext requireSession(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new BaseException(ResultCode.PARAM_ERROR, "sessionId must not be null or blank");
        }
        SessionContext ctx = sessions.get(sessionId);
        if (ctx == null) {
            throw new BaseException(ResultCode.AGENT_NOT_FOUND,
                    "Codex session not found: " + sessionId);
        }
        return ctx;
    }

    private void validateConfiguration() {
        if (!config.isEnabled()) {
            throw new BaseException(ResultCode.AGENT_EXECUTION_FAILED,
                    "External agent adapters are disabled. Set agent.external.enabled=true to enable.");
        }
    }

    private List<String> buildCodexCommand(SessionContext ctx, String message) {
        List<String> cmd = new ArrayList<>();
        cmd.add("codex");

        String model = config.getModel();
        if (model == null || model.isBlank()) {
            model = DEFAULT_MODEL;
        }
        cmd.add("--model");
        cmd.add(model);

        if (config.getApiKey() != null && !config.getApiKey().isBlank()) {
            cmd.add("--api-key");
            cmd.add(config.getApiKey());
        }

        if (config.getBaseUrl() != null && !config.getBaseUrl().isBlank()) {
            cmd.add("--base-url");
            cmd.add(config.getBaseUrl());
        }

        // Write the prompt to a temp file to avoid shell escaping issues
        Path promptFile = ctx.workspace.resolve("prompt_" + UUID.randomUUID() + ".txt");
        try {
            Files.writeString(promptFile, message);
            cmd.add("--prompt-file");
            cmd.add(promptFile.toString());
        } catch (IOException e) {
            log.warn("Failed to write prompt file, falling back to inline prompt: {}", e.getMessage());
            cmd.add(message);
        }

        return cmd;
    }

    private static void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        if (Files.isDirectory(path)) {
            try (var stream = Files.list(path)) {
                stream.forEach(child -> {
                    try {
                        deleteRecursively(child);
                    } catch (IOException e) {
                        // best-effort
                    }
                });
            }
        }
        Files.delete(path);
    }

    /** Per-session state. */
    private static final class SessionContext {
        final String sessionId;
        final Path workspace;
        final List<AgentEvent> events = Collections.synchronizedList(new ArrayList<>());

        SessionContext(String sessionId, Path workspace) {
            this.sessionId = sessionId;
            this.workspace = workspace;
        }
    }
}
