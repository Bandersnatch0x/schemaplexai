package com.schemaplexai.agent.engine.reasoning;

import com.schemaplexai.agent.engine.tool.sandbox.SandboxException;
import com.schemaplexai.agent.engine.tool.sandbox.SandboxProvider;
import com.schemaplexai.agent.engine.tool.sandbox.SandboxSession;
import com.schemaplexai.agent.engine.tool.sandbox.SandboxSessionConfig;
import com.schemaplexai.agent.engine.tool.sandbox.ShellCommand;
import com.schemaplexai.agent.engine.tool.sandbox.ShellResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Executes code snippets safely in a sandboxed environment for reasoning.
 * Supports Python and JavaScript execution via the {@link SandboxProvider} abstraction.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CodeExecutionReasoner {

    private static final Set<String> PYTHON_FORBIDDEN_KEYWORDS = Set.of(
            "__import__", "eval", "exec", "compile", "open", "os.system",
            "subprocess", "sys.exit", "breakpoint", "input"
    );

    private static final Set<String> JS_FORBIDDEN_KEYWORDS = Set.of(
            "eval", "Function", "setTimeout", "setInterval", "require",
            "process", "child_process", "fs", "http", "https", "net"
    );

    private static final Pattern IMPORT_PATTERN = Pattern.compile(
            "^\\s*import\\s+\\w+|^\\s*from\\s+\\w+\\s+import|^\\s*const\\s+\\w+\\s*=\\s*require"
    );

    private final SandboxProvider sandboxProvider;

    /**
     * Executes a Python code snippet in a sandboxed environment.
     *
     * @param code the Python code to execute
     * @return the {@link ShellResult} containing stdout, stderr, and exit code
     * @throws IllegalArgumentException if the code is deemed unsafe
     * @throws SandboxException         if sandbox execution fails
     */
    public ShellResult executePython(String code) throws SandboxException {
        if (!isSafeCode(code, Language.PYTHON)) {
            throw new IllegalArgumentException("Python code failed safety check");
        }

        try (SandboxSession session = sandboxProvider.create(SandboxSessionConfig.defaults())) {
            session.writeFile(Path.of("script.py"), code.getBytes());
            ShellCommand command = ShellCommand.of("python", "script.py");
            ShellResult result = session.exec(command);
            log.debug("Python execution exitCode={}, stdout length={}",
                    result.exitCode(), result.stdout().length());
            return result;
        }
    }

    /**
     * Executes a JavaScript code snippet in a sandboxed environment.
     *
     * @param code the JavaScript code to execute
     * @return the {@link ShellResult} containing stdout, stderr, and exit code
     * @throws IllegalArgumentException if the code is deemed unsafe
     * @throws SandboxException         if sandbox execution fails
     */
    public ShellResult executeJavaScript(String code) throws SandboxException {
        if (!isSafeCode(code, Language.JAVASCRIPT)) {
            throw new IllegalArgumentException("JavaScript code failed safety check");
        }

        try (SandboxSession session = sandboxProvider.create(SandboxSessionConfig.defaults())) {
            session.writeFile(Path.of("script.js"), code.getBytes());
            ShellCommand command = ShellCommand.of("node", "script.js");
            ShellResult result = session.exec(command);
            log.debug("JavaScript execution exitCode={}, stdout length={}",
                    result.exitCode(), result.stdout().length());
            return result;
        }
    }

    /**
     * Determines whether the given code is safe to execute.
     * Checks against forbidden keywords from both Python and JavaScript,
     * as well as import/require patterns.
     *
     * @param code the code to inspect
     * @return true if the code passes all safety heuristics
     */
    public boolean isSafeCode(String code) {
        if (code == null || code.isBlank()) {
            return false;
        }

        String normalized = code.toLowerCase();

        // Check all forbidden keywords from both languages
        for (String keyword : PYTHON_FORBIDDEN_KEYWORDS) {
            if (normalized.contains(keyword.toLowerCase())) {
                log.warn("Safety check failed: forbidden keyword '{}' found", keyword);
                return false;
            }
        }
        for (String keyword : JS_FORBIDDEN_KEYWORDS) {
            if (normalized.contains(keyword.toLowerCase())) {
                log.warn("Safety check failed: forbidden keyword '{}' found", keyword);
                return false;
            }
        }

        // Block import/require statements that could load arbitrary modules
        if (IMPORT_PATTERN.matcher(code).find()) {
            log.warn("Safety check failed: import/require statement detected");
            return false;
        }

        return true;
    }

    private boolean isSafeCode(String code, Language language) {
        if (code == null || code.isBlank()) {
            return false;
        }

        String normalized = code.toLowerCase();

        Set<String> forbidden = switch (language) {
            case PYTHON -> PYTHON_FORBIDDEN_KEYWORDS;
            case JAVASCRIPT -> JS_FORBIDDEN_KEYWORDS;
        };

        for (String keyword : forbidden) {
            if (normalized.contains(keyword.toLowerCase())) {
                log.warn("Safety check failed: forbidden keyword '{}' found in {} code",
                        keyword, language);
                return false;
            }
        }

        // Block import/require statements that could load arbitrary modules
        if (IMPORT_PATTERN.matcher(code).find()) {
            log.warn("Safety check failed: import/require statement detected in {} code", language);
            return false;
        }

        return true;
    }

    private enum Language {
        PYTHON,
        JAVASCRIPT
    }
}
