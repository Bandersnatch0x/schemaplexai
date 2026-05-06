package com.schemaplexai.common.manifest;

/**
 * AGENTS.md 解析失败时抛出。运行时异常 — 调用方通过 try/catch 处理，避免污染调用链 signature。
 *
 * <p>错误来源包括：缺少 frontmatter、必填字段（{@code name}）缺失、YAML 语法错误、字段类型错误等。
 */
public class ManifestParseException extends RuntimeException {

    public ManifestParseException(String message) {
        super(message);
    }

    public ManifestParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
