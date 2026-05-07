package com.schemaplexai.agent.engine.tool.sandbox.provider;

import com.schemaplexai.agent.engine.tool.sandbox.ShellResult;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Compile-safe adapter that abstracts Docker container operations.
 *
 * <p>This adapter mirrors the Docker Java API surface needed by
 * {@link ContainerSandboxProvider} without requiring the
 * {@code com.github.docker-java:docker-java} dependency on the classpath.
 *
 * <p>If the real Docker Java API is present, this adapter delegates to it
 * via reflection. Otherwise it acts as a no-op stub and
 * {@link #isAvailable()} returns {@code false}.
 */
@Slf4j
public class DockerClientAdapter {

    private static final boolean DOCKER_JAVA_PRESENT;
    private static final Object DOCKER_CLIENT;

    static {
        boolean present;
        Object client = null;
        try {
            Class<?> dockerClientClass = Class.forName("com.github.dockerjava.api.DockerClient");
            Class<?> dockerClientBuilderClass = Class.forName("com.github.dockerjava.core.DockerClientBuilder");
            Object builder = dockerClientBuilderClass.getMethod("getInstance").invoke(null);
            client = dockerClientBuilderClass.getMethod("build").invoke(builder);
            present = true;
        } catch (Exception e) {
            present = false;
            log.debug("Docker Java API not available on classpath: {}", e.getMessage());
        }
        DOCKER_JAVA_PRESENT = present;
        DOCKER_CLIENT = client;
    }

    /** Creates a new adapter instance. */
    public static DockerClientAdapter create() {
        return new DockerClientAdapter();
    }

    /** Returns {@code true} if the Docker Java API is available. */
    public boolean isAvailable() {
        return DOCKER_JAVA_PRESENT && DOCKER_CLIENT != null;
    }

    /**
     * Creates a Docker container from the given options.
     *
     * @return the container ID
     * @throws Exception if creation fails
     */
    public String createContainer(DockerCreateOptions options) throws Exception {
        if (!isAvailable()) {
            throw new IllegalStateException("Docker Java API is not available");
        }
        // Reflective delegation to Docker Java API
        Object hostConfig = buildHostConfig(options);
        Object createCmd = invoke(DOCKER_CLIENT, "createContainerCmd", options.getImage());
        invoke(createCmd, "withHostConfig", hostConfig);
        if (options.getEnv() != null && !options.getEnv().isEmpty()) {
            String[] envArray = options.getEnv().entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .toArray(String[]::new);
            invoke(createCmd, "withEnv", (Object) envArray);
        }
        Object response = invoke(createCmd, "exec");
        return (String) invoke(response, "getId");
    }

    /** Starts the container with the given ID. */
    public void startContainer(String containerId) throws Exception {
        if (!isAvailable()) {
            throw new IllegalStateException("Docker Java API is not available");
        }
        Object startCmd = invoke(DOCKER_CLIENT, "startContainerCmd", containerId);
        invoke(startCmd, "exec");
    }

    /** Stops the container with the given ID. */
    public void stopContainer(String containerId) throws Exception {
        if (!isAvailable()) {
            return;
        }
        try {
            Object stopCmd = invoke(DOCKER_CLIENT, "stopContainerCmd", containerId);
            invoke(stopCmd, "withTimeout", 10);
            invoke(stopCmd, "exec");
        } catch (Exception e) {
            log.warn("Failed to stop container {}, attempting kill: {}", containerId, e.getMessage());
            try {
                Object killCmd = invoke(DOCKER_CLIENT, "killContainerCmd", containerId);
                invoke(killCmd, "exec");
            } catch (Exception killEx) {
                log.warn("Failed to kill container {}: {}", containerId, killEx.getMessage());
            }
        }
    }

    /** Removes the container with the given ID. */
    public void removeContainer(String containerId) throws Exception {
        if (!isAvailable()) {
            return;
        }
        try {
            Object removeCmd = invoke(DOCKER_CLIENT, "removeContainerCmd", containerId);
            invoke(removeCmd, "withForce", true);
            invoke(removeCmd, "exec");
        } catch (Exception e) {
            log.warn("Failed to remove container {}: {}", containerId, e.getMessage());
        }
    }

    /**
     * Executes a command inside the container.
     *
     * @return the shell result
     * @throws Exception if execution fails
     */
    public ShellResult execInContainer(String containerId, List<String> argv,
                                       Map<String, String> env, String workingDir,
                                       Duration timeout) throws Exception {
        if (!isAvailable()) {
            throw new IllegalStateException("Docker Java API is not available");
        }
        Object execCmd = invoke(DOCKER_CLIENT, "execCreateCmd", containerId);
        invoke(execCmd, "withCmd", (Object) argv.toArray(new String[0]));
        invoke(execCmd, "withAttachStdout", true);
        invoke(execCmd, "withAttachStderr", true);
        if (workingDir != null) {
            invoke(execCmd, "withWorkingDir", workingDir);
        }
        Object execResponse = invoke(execCmd, "exec");
        String execId = (String) invoke(execResponse, "getId");

        Object startCmd = invoke(DOCKER_CLIENT, "execStartCmd", execId);
        // Use FrameReader to collect output
        StringBuilder stdout = new StringBuilder();
        StringBuilder stderr = new StringBuilder();

        Object callback = createExecCallback(stdout, stderr);
        invoke(startCmd, "exec", callback);

        // Wait for completion with timeout
        boolean finished = waitForExec(execId, timeout);
        boolean timedOut = !finished;
        int exitCode = timedOut ? -1 : inspectExecExitCode(execId);

        if (timedOut) {
            log.warn("Container exec timed out for container {}", containerId);
        }

        return new ShellResult(
                exitCode,
                stdout.toString(),
                stderr.toString(),
                timeout,
                timedOut
        );
    }

    // --- private helpers ---

    private Object buildHostConfig(DockerCreateOptions options) throws Exception {
        Class<?> hostConfigClass = Class.forName("com.github.dockerjava.api.model.HostConfig");
        Object hostConfig = hostConfigClass.getDeclaredConstructor().newInstance();

        // Network mode
        if (options.getNetworkMode() != null) {
            invoke(hostConfig, "withNetworkMode", options.getNetworkMode());
        }
        // Read-only rootfs
        invoke(hostConfig, "withReadonlyRootfs", options.isReadOnlyRootfs());
        // Memory limit
        invoke(hostConfig, "withMemory", options.getMemoryLimitBytes());
        // CPU quota
        if (options.getCpuQuotaMicros() > 0) {
            invoke(hostConfig, "withCpuQuota", (long) options.getCpuQuotaMicros());
        }
        // Auto remove
        invoke(hostConfig, "withAutoRemove", options.isAutoRemove());
        // Bind mounts
        if (options.getBinds() != null && !options.getBinds().isEmpty()) {
            Class<?> bindClass = Class.forName("com.github.dockerjava.api.model.Bind");
            Class<?> volumeClass = Class.forName("com.github.dockerjava.api.model.Volume");
            Object[] binds = options.getBinds().stream().map(b -> {
                try {
                    Object volume = volumeClass.getConstructor(String.class).newInstance(b.getContainerPath());
                    Object bind = bindClass.getMethod("parse", String.class)
                            .invoke(null, b.getHostPath() + ":" + b.getContainerPath() + (b.isReadOnly() ? ":ro" : ""));
                    return bind;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).toArray();
            invoke(hostConfig, "withBinds", (Object) binds);
        }
        return hostConfig;
    }

    private Object createExecCallback(StringBuilder stdout, StringBuilder stderr) throws Exception {
        Class<?> resultCallbackClass = Class.forName("com.github.dockerjava.api.async.ResultCallback");
        Class<?> frameClass = Class.forName("com.github.dockerjava.api.model.Frame");
        Class<?> streamTypeClass = Class.forName("com.github.dockerjava.api.model.StreamType");

        return java.lang.reflect.Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[]{resultCallbackClass},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("onNext".equals(name) && args != null && args.length > 0) {
                        Object frame = args[0];
                        Object streamType = invoke(frame, "getStreamType");
                        String payload = (String) invoke(frame, "toString");
                        Object stdoutType = streamTypeClass.getField("STDOUT").get(null);
                        if (streamType == stdoutType) {
                            stdout.append(payload);
                        } else {
                            stderr.append(payload);
                        }
                    }
                    if ("onError".equals(name) && args != null && args.length > 0) {
                        log.warn("Exec error: {}", args[0]);
                    }
                    return null;
                }
        );
    }

    private boolean waitForExec(String execId, Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            try {
                Integer exitCode = inspectExecExitCode(execId);
                if (exitCode != null) {
                    return true;
                }
            } catch (Exception e) {
                // still running
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    private Integer inspectExecExitCode(String execId) throws Exception {
        Object inspectCmd = invoke(DOCKER_CLIENT, "inspectExecCmd", execId);
        Object response = invoke(inspectCmd, "exec");
        return (Integer) invoke(response, "getExitCodeLong");
    }

    private static Object invoke(Object target, String methodName, Object... args) throws Exception {
        Class<?> clazz = target.getClass();
        java.lang.reflect.Method method = findMethod(clazz, methodName, args);
        if (method == null) {
            throw new NoSuchMethodException(methodName + " not found on " + clazz.getName());
        }
        return method.invoke(target, args);
    }

    private static java.lang.reflect.Method findMethod(Class<?> clazz, String name, Object[] args) {
        for (java.lang.reflect.Method m : clazz.getMethods()) {
            if (!m.getName().equals(name)) {
                continue;
            }
            Class<?>[] params = m.getParameterTypes();
            if (params.length != args.length) {
                continue;
            }
            boolean match = true;
            for (int i = 0; i < params.length; i++) {
                if (args[i] != null && !isAssignable(params[i], args[i].getClass())) {
                    match = false;
                    break;
                }
            }
            if (match) {
                return m;
            }
        }
        return null;
    }

    private static boolean isAssignable(Class<?> param, Class<?> arg) {
        if (param.isAssignableFrom(arg)) {
            return true;
        }
        if (param.isPrimitive()) {
            if (param == boolean.class && arg == Boolean.class) return true;
            if (param == long.class && arg == Long.class) return true;
            if (param == int.class && arg == Integer.class) return true;
        }
        return false;
    }
}
