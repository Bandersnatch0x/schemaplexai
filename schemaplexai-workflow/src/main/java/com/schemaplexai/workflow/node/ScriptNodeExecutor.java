package com.schemaplexai.workflow.node;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Executes SCRIPT nodes in-process (spec §3.4).
 *
 * <p>Supported languages: {@code groovy} (via an embedded GroovyShell) and
 * {@code javascript} (via the standalone Nashorn engine; the JDK no longer bundles a JS
 * engine). The script sees the node input as {@code input}, the tenant as
 * {@code tenantId}, and may return a value that becomes {@code output.result}.
 *
 * <p>Each script runs on a daemon worker thread bounded by {@code timeoutSeconds}
 * (default 60 per spec §3.4); exceeding the budget yields the TIMEOUT terminal state.
 *
 * <p><b>SANDBOX DEBT (ticket 923):</b> this is in-process execution, not a sandbox.
 * Scripts can still touch reflection, the filesystem and the network; there is no class
 * whitelist (spec §3.4 security constraints). A real sandbox (Groovy SecureAST /
 * Nashorn ClassFilter with an allowlist, or an out-of-process runner) is a known follow-up.
 */
@Slf4j
@Component
public class ScriptNodeExecutor implements NodeExecutor {

    private static final int DEFAULT_TIMEOUT_SECONDS = 60;

    private final ExecutorService scriptExecutor = Executors.newCachedThreadPool(daemonThreadFactory());
    private final ScriptEngineManager engineManager = new ScriptEngineManager();

    @Override
    public String getNodeType() {
        return "SCRIPT";
    }

    @Override
    public NodeExecutionResult execute(Map<String, Object> input, String tenantId) {
        String script = (String) input.get("script");
        if (script == null || script.isBlank()) {
            return NodeExecutionResult.failure("Missing or empty required field: script");
        }
        String language = (String) input.get("language");
        if (language == null || language.isBlank()) {
            return NodeExecutionResult.failure("Missing or empty required field: language");
        }

        String normalizedLanguage = language.trim().toLowerCase();
        int timeoutSeconds = readTimeoutSeconds(input);

        Callable<Object> task = switch (normalizedLanguage) {
            case "groovy" -> () -> runGroovy(script, input, tenantId);
            case "javascript" -> () -> runJavaScript(script, input, tenantId);
            default -> null;
        };
        if (task == null) {
            return NodeExecutionResult.failure(
                    "Unsupported script language: " + language + " (supported: groovy, javascript)");
        }

        Future<Object> future = scriptExecutor.submit(task);
        try {
            Object value = future.get(timeoutSeconds, TimeUnit.SECONDS);
            Map<String, Object> output = new HashMap<>();
            output.put("result", value);
            output.put("language", normalizedLanguage);
            log.info("SCRIPT node executed: language={}, timeoutSeconds={}, tenantId={}",
                    normalizedLanguage, timeoutSeconds, tenantId);
            return NodeExecutionResult.success(output);
        } catch (TimeoutException e) {
            future.cancel(true);
            log.warn("SCRIPT node timed out after {}s (language={})", timeoutSeconds, normalizedLanguage);
            return NodeExecutionResult.timeout("Script execution timed out after " + timeoutSeconds + "s");
        } catch (InterruptedException e) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            return NodeExecutionResult.failure("Script execution interrupted");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            log.error("SCRIPT node failed (language={}): {}", normalizedLanguage, cause.getMessage());
            return NodeExecutionResult.failure("Script execution failed: " + cause.getMessage());
        }
    }

    private Object runGroovy(String script, Map<String, Object> input, String tenantId) {
        Binding binding = new Binding();
        binding.setVariable("input", input);
        binding.setVariable("tenantId", tenantId);
        return new GroovyShell(binding).evaluate(script);
    }

    private Object runJavaScript(String script, Map<String, Object> input, String tenantId) throws Exception {
        ScriptEngine engine = engineManager.getEngineByName("nashorn");
        if (engine == null) {
            engine = engineManager.getEngineByName("javascript");
        }
        if (engine == null) {
            throw new IllegalStateException("No JavaScript engine available on the classpath");
        }
        Bindings bindings = engine.createBindings();
        bindings.put("input", input);
        bindings.put("tenantId", tenantId);
        return engine.eval(script, bindings);
    }

    private int readTimeoutSeconds(Map<String, Object> input) {
        Object raw = input.get("timeoutSeconds");
        if (raw instanceof Number number) {
            int value = number.intValue();
            if (value > 0) {
                return value;
            }
        }
        return DEFAULT_TIMEOUT_SECONDS;
    }

    private static ThreadFactory daemonThreadFactory() {
        AtomicInteger counter = new AtomicInteger();
        return runnable -> {
            Thread thread = new Thread(runnable, "workflow-script-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }
}
