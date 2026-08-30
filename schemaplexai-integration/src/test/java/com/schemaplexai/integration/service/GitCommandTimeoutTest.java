package com.schemaplexai.integration.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaplexai.common.exception.BaseException;
import com.schemaplexai.common.result.ResultCode;
import com.schemaplexai.integration.security.IntegrationCredentialEncryptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Issue 918: git CLI invocations are bounded by a timeout; on expiry the process
 * is destroyed and the call degrades to a structured REQUEST_TIMEOUT failure
 * instead of blocking forever.
 */
@ExtendWith(MockitoExtension.class)
class GitCommandTimeoutTest {

    private static final Long TENANT_ID = 1L;

    @Mock
    private RestTemplate restTemplate;

    private GitIntegrationService gitService;

    @TempDir
    Path workingDir;

    @BeforeEach
    void setUp() {
        gitService = new GitIntegrationService(new ObjectMapper(), restTemplate,
                new IntegrationCredentialEncryptor("timeout-test-secret"),
                GitIntegrationServiceTest.oauthProperties());
        gitService.clearStore();
    }

    private Process hungProcess() throws Exception {
        Process process = mock(Process.class);
        when(process.getInputStream()).thenReturn(emptyStream());
        when(process.waitFor(anyLong(), any(TimeUnit.class))).thenReturn(false);
        when(process.destroyForcibly()).thenReturn(process);
        return process;
    }

    private InputStream emptyStream() {
        return new ByteArrayInputStream(new byte[0]);
    }

    @Test
    void executeGitCommand_timeout_destroysProcessAndThrowsRequestTimeout() throws Exception {
        GitIntegrationService spyService = spy(gitService);
        Process hung = hungProcess();
        doReturn(hung).when(spyService).startProcess(anyList(), any(File.class));

        assertThatThrownBy(() -> spyService.executeGitCommandInDir(workingDir.toString(), "clone", "url", "dest"))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.REQUEST_TIMEOUT.getCode());

        verify(hung).waitFor(anyLong(), any(TimeUnit.class));
        verify(hung).destroyForcibly();
    }

    @Test
    void listBranches_gitTimeout_degradesToRequestTimeout() throws Exception {
        GitIntegrationService spyService = spy(gitService);
        Process hung = hungProcess();
        doReturn(hung).when(spyService).startProcess(anyList(), any(File.class));

        assertThatThrownBy(() -> spyService.listBranches(TENANT_ID, 1L, workingDir.toString()))
                .isInstanceOf(BaseException.class)
                .extracting("code")
                .isEqualTo(ResultCode.REQUEST_TIMEOUT.getCode());

        verify(hung).destroyForcibly();
    }

    @Test
    void executeGitCommand_success_returnsOutput() throws Exception {
        GitIntegrationService spyService = spy(gitService);
        Process ok = mock(Process.class);
        when(ok.getInputStream()).thenReturn(new ByteArrayInputStream("branch-a\nbranch-b\n".getBytes()));
        when(ok.waitFor(anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(ok.exitValue()).thenReturn(0);
        doReturn(ok).when(spyService).startProcess(anyList(), any(File.class));

        String output = spyService.executeGitCommandInDir(workingDir.toString(), "branch", "-a");

        assertThat(output).contains("branch-a").contains("branch-b");
    }

    @Test
    void executeGitCommand_nonZeroExit_throws() throws Exception {
        GitIntegrationService spyService = spy(gitService);
        Process failed = mock(Process.class);
        when(failed.getInputStream()).thenReturn(emptyStream());
        when(failed.waitFor(anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(failed.exitValue()).thenReturn(128);
        doReturn(failed).when(spyService).startProcess(anyList(), any(File.class));

        assertThatThrownBy(() -> spyService.executeGitCommandInDir(workingDir.toString(), "status"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("128");
    }

    @Test
    void listBranches_success_parsesBranchLines() throws Exception {
        GitIntegrationService spyService = spy(gitService);
        Process ok = mock(Process.class);
        when(ok.getInputStream()).thenReturn(new ByteArrayInputStream("* main\n  remotes/origin/dev\n".getBytes()));
        when(ok.waitFor(anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(ok.exitValue()).thenReturn(0);
        doReturn(ok).when(spyService).startProcess(anyList(), any(File.class));

        List<String> branches = spyService.listBranches(TENANT_ID, 1L, workingDir.toString());

        assertThat(branches).containsExactly("main", "remotes/origin/dev");
    }
}
