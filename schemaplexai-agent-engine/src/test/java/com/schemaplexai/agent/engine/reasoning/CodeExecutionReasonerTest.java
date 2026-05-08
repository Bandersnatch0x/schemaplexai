package com.schemaplexai.agent.engine.reasoning;

import com.schemaplexai.agent.engine.tool.sandbox.SandboxException;
import com.schemaplexai.agent.engine.tool.sandbox.SandboxProvider;
import com.schemaplexai.agent.engine.tool.sandbox.SandboxSession;
import com.schemaplexai.agent.engine.tool.sandbox.SandboxSessionConfig;
import com.schemaplexai.agent.engine.tool.sandbox.ShellCommand;
import com.schemaplexai.agent.engine.tool.sandbox.ShellResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CodeExecutionReasoner Tests")
class CodeExecutionReasonerTest {

    @Mock
    private SandboxProvider sandboxProvider;

    @Mock
    private SandboxSession sandboxSession;

    private CodeExecutionReasoner reasoner;

    @BeforeEach
    void setUp() {
        reasoner = new CodeExecutionReasoner(sandboxProvider);
    }

    // --- isSafeCode ---

    @Test
    @DisplayName("isSafeCode returns true for benign code")
    void safeCode() {
        assertTrue(reasoner.isSafeCode("print('hello')"));
        assertTrue(reasoner.isSafeCode("1 + 1"));
        assertTrue(reasoner.isSafeCode("const x = 42; console.log(x);"));
    }

    @Test
    @DisplayName("isSafeCode returns false for null or blank")
    void nullBlankCode() {
        assertFalse(reasoner.isSafeCode(null));
        assertFalse(reasoner.isSafeCode(""));
        assertFalse(reasoner.isSafeCode("   "));
    }

    @Test
    @DisplayName("isSafeCode blocks unsafe Python keywords")
    void unsafePythonKeywords() {
        assertFalse(reasoner.isSafeCode("__import__('os')"));
        assertFalse(reasoner.isSafeCode("eval('1+1')"));
        assertFalse(reasoner.isSafeCode("exec('pass')"));
        assertFalse(reasoner.isSafeCode("os.system('ls')"));
    }

    @Test
    @DisplayName("isSafeCode blocks unsafe JavaScript keywords")
    void unsafeJavaScriptKeywords() {
        assertFalse(reasoner.isSafeCode("require('fs')"));
        assertFalse(reasoner.isSafeCode("eval('1+1')"));
        assertFalse(reasoner.isSafeCode("fetch('http://evil.com')"));
    }

    @Test
    @DisplayName("isSafeCode blocks import statements")
    void importStatementsBlocked() {
        assertFalse(reasoner.isSafeCode("import os"));
        assertFalse(reasoner.isSafeCode("from sys import exit"));
        assertFalse(reasoner.isSafeCode("const fs = require('fs')"));
    }

    // --- executePython ---

    @Test
    @DisplayName("executePython runs safe code successfully")
    void executePythonSuccess() throws Exception {
        when(sandboxProvider.create(any(SandboxSessionConfig.class))).thenReturn(sandboxSession);
        when(sandboxSession.exec(any(ShellCommand.class)))
                .thenReturn(new ShellResult(0, "hello\n", "", Duration.ZERO, false));

        ShellResult result = reasoner.executePython("print('hello')");

        assertEquals(0, result.exitCode());
        assertEquals("hello\n", result.stdout());
        verify(sandboxSession).writeFile(eq(Path.of("script.py")), eq("print('hello')".getBytes()));
        verify(sandboxSession).close();
    }

    @Test
    @DisplayName("executePython throws for unsafe code")
    void executePythonUnsafe() {
        assertThrows(IllegalArgumentException.class,
                () -> reasoner.executePython("os.system('rm -rf /')"));
        verifyNoInteractions(sandboxProvider);
    }

    @Test
    @DisplayName("executePython returns ShellResult with error on execution failure")
    void executePythonFailure() throws Exception {
        when(sandboxProvider.create(any(SandboxSessionConfig.class))).thenReturn(sandboxSession);
        when(sandboxSession.exec(any(ShellCommand.class)))
                .thenReturn(new ShellResult(1, "", "SyntaxError", Duration.ZERO, false));

        ShellResult result = reasoner.executePython("bad syntax");

        assertEquals(1, result.exitCode());
        assertEquals("SyntaxError", result.stderr());
        verify(sandboxSession).close();
    }

    // --- executeJavaScript ---

    @Test
    @DisplayName("executeJavaScript runs safe code successfully")
    void executeJavaScriptSuccess() throws Exception {
        when(sandboxProvider.create(any(SandboxSessionConfig.class))).thenReturn(sandboxSession);
        when(sandboxSession.exec(any(ShellCommand.class)))
                .thenReturn(new ShellResult(0, "42\n", "", Duration.ZERO, false));

        ShellResult result = reasoner.executeJavaScript("console.log(42)");

        assertEquals(0, result.exitCode());
        assertEquals("42\n", result.stdout());
        verify(sandboxSession).writeFile(eq(Path.of("script.js")), eq("console.log(42)".getBytes()));
        verify(sandboxSession).close();
    }

    @Test
    @DisplayName("executeJavaScript throws for unsafe code")
    void executeJavaScriptUnsafe() {
        assertThrows(IllegalArgumentException.class,
                () -> reasoner.executeJavaScript("require('child_process')"));
        verifyNoInteractions(sandboxProvider);
    }

    @Test
    @DisplayName("executeJavaScript returns ShellResult with error on execution failure")
    void executeJavaScriptFailure() throws Exception {
        when(sandboxProvider.create(any(SandboxSessionConfig.class))).thenReturn(sandboxSession);
        when(sandboxSession.exec(any(ShellCommand.class)))
                .thenReturn(new ShellResult(1, "", "ReferenceError", Duration.ZERO, false));

        ShellResult result = reasoner.executeJavaScript("undefinedVar");

        assertEquals(1, result.exitCode());
        assertEquals("ReferenceError", result.stderr());
        verify(sandboxSession).close();
    }

    // --- session lifecycle ---

    @Test
    @DisplayName("session is closed even on exception")
    void sessionClosedOnException() throws Exception {
        when(sandboxProvider.create(any(SandboxSessionConfig.class))).thenReturn(sandboxSession);
        when(sandboxSession.exec(any(ShellCommand.class)))
                .thenThrow(new RuntimeException("boom"));

        assertThrows(RuntimeException.class, () -> reasoner.executePython("print(1)"));
        verify(sandboxSession).close();
    }
}
