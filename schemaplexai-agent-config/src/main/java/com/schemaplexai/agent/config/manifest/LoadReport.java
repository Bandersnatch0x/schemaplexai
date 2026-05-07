package com.schemaplexai.agent.config.manifest;

import java.util.List;

/**
 * AGENTS.md 批量加载报告。
 *
 * @param results 每条 LoadResult
 */
public record LoadReport(List<LoadResult> results) {

    public LoadReport {
        results = results == null ? List.of() : List.copyOf(results);
    }

    public long okCount() {
        return results.stream().filter(LoadResult::success).count();
    }

    public long failedCount() {
        return results.stream().filter(r -> !r.success()).count();
    }
}
