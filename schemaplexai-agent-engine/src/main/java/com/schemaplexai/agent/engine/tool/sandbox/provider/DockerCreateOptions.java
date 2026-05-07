package com.schemaplexai.agent.engine.tool.sandbox.provider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Compile-safe value object describing Docker container creation options.
 *
 * <p>Mirrors the subset of {@code com.github.dockerjava.api.command.CreateContainerCmd}
 * fields used by {@link ContainerSandboxProvider}.
 */
public class DockerCreateOptions {

    private String image;
    private String networkMode;
    private boolean readOnlyRootfs;
    private long memoryLimitBytes;
    private long cpuQuotaMicros;
    private boolean autoRemove;
    private Map<String, String> env;
    private final List<BindMount> binds = new ArrayList<>();

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getNetworkMode() {
        return networkMode;
    }

    public void setNetworkMode(String networkMode) {
        this.networkMode = networkMode;
    }

    public boolean isReadOnlyRootfs() {
        return readOnlyRootfs;
    }

    public void setReadOnlyRootfs(boolean readOnlyRootfs) {
        this.readOnlyRootfs = readOnlyRootfs;
    }

    public long getMemoryLimitBytes() {
        return memoryLimitBytes;
    }

    public void setMemoryLimitBytes(long memoryLimitBytes) {
        this.memoryLimitBytes = memoryLimitBytes;
    }

    public long getCpuQuotaMicros() {
        return cpuQuotaMicros;
    }

    public void setCpuQuotaMicros(long cpuQuotaMicros) {
        this.cpuQuotaMicros = cpuQuotaMicros;
    }

    public boolean isAutoRemove() {
        return autoRemove;
    }

    public void setAutoRemove(boolean autoRemove) {
        this.autoRemove = autoRemove;
    }

    public Map<String, String> getEnv() {
        return env;
    }

    public void setEnv(Map<String, String> env) {
        this.env = env;
    }

    public List<BindMount> getBinds() {
        return List.copyOf(binds);
    }

    public void addBindMount(String hostPath, String containerPath, boolean readOnly) {
        binds.add(new BindMount(hostPath, containerPath, readOnly));
    }

    /**
     * A single bind mount declaration.
     */
    public static class BindMount {
        private final String hostPath;
        private final String containerPath;
        private final boolean readOnly;

        public BindMount(String hostPath, String containerPath, boolean readOnly) {
            this.hostPath = hostPath;
            this.containerPath = containerPath;
            this.readOnly = readOnly;
        }

        public String getHostPath() {
            return hostPath;
        }

        public String getContainerPath() {
            return containerPath;
        }

        public boolean isReadOnly() {
            return readOnly;
        }
    }
}
