---
title: ToolSafetyGuard
type: tool
source: schemaplexai-agent-engine/src/main/java/com/schemaplexai/agent/engine/tool/ToolSafetyGuard.java
creation_date: 2026-05-01
update_date: 2026-05-01
tags: [tool, security, safety-guard, agent-engine]
confidence: high
---

# ToolSafetyGuard

> One-sentence summary: Pre-execution security guard that evaluates tool calls against a multi-dimensional safety policy before any tool is allowed to run.

## Responsibilities

1. **Tool name blacklist** — Block known irreversible operations by name
2. **Argument content scan** — Detect destructive commands embedded in parameters
3. **Environment mismatch detection** — Prevent cross-environment operations
4. **Input normalization** — Defeat obfuscation attacks (Unicode homoglyphs, HTML entities, JSON escapes)

## Safety Dimensions

| Dimension | Implementation | Example Blocked |
|-----------|----------------|-----------------|
| Irreversible tool names | `Set.contains()` | `volumeDelete`, `databaseDrop` |
| Destructive commands in args | Regex after normalization | `DROP TABLE`, `RM -RF` |
| Environment mismatch | String containment check | `prod` argument in non-prod tenant |
| Obfuscation bypass | `normalizeInput()` pipeline | Cyrillic `о` in `DRоP TABLE` |

## Key Code

```java
@Component
public class ToolSafetyGuard {

    public SafetyCheckResult check(String toolName, String arguments, String expectedEnvironment) {
        // Check 1: Irreversible tool names
        if (IRREVERSIBLE_TOOLS.contains(toolName)) {
            return SafetyCheckResult.reject(ToolErrorCategory.IRREVERSIBLE_OPERATION, ...);
        }

        // Check 2: Destructive commands in arguments (with normalization)
        String normalized = normalizeInput(arguments);
        if (IRREVERSIBLE_PATTERN.matcher(normalized).find()) {
            return SafetyCheckResult.reject(ToolErrorCategory.IRREVERSIBLE_OPERATION, ...);
        }

        // Check 3: Environment/credential mismatch
        if (expectedEnvironment != null && arguments != null) {
            if (arguments.contains("prod") && !expectedEnvironment.contains("prod")) {
                return SafetyCheckResult.reject(ToolErrorCategory.ENVIRONMENT_MISMATCH, ...);
            }
        }

        return SafetyCheckResult.permit();
    }
}
```

## Input Normalization Pipeline

```
Raw Input
    |
    v
decodeJsonEscapes()   // \\u0020 → space
    |
    v
decodeHtmlEntities()  // &#68; → D
    |
    v
Normalizer.normalize(NFKC)  // Unicode canonical decomposition + composition
    |
    v
mapHomoglyphs()       // Cyrillic а, е, о, р, с, х → Latin a, e, o, p, c, x
    |
    v
collapseWhitespace()  // \\s+ → single space
    |
    v
Normalized (ready for regex matching)
```

## Obfuscation Defenses

| Attack Technique | Example | Defense |
|------------------|---------|---------|
| Unicode homoglyphs | `DRоP` (Cyrillic о U+043E) | `mapHomoglyphs()` → `DROP` |
| HTML entities | `&#68;ROP` | `decodeHtmlEntities()` → `DROP` |
| JSON escapes | `DROP\\u0020TABLE` | `decodeJsonEscapes()` → `DROP TABLE` |
| Extra whitespace | `DROP   TABLE` | `collapseWhitespace()` → `DROP TABLE` |
| Newline separation | `DROP\\nTABLE` | `collapseWhitespace()` → `DROP TABLE` |

## Result

```java
public record SafetyCheckResult(
    boolean allowed,
    boolean blocked,
    ToolErrorCategory errorCategory,
    String reason
) {
    public static SafetyCheckResult permit() { ... }
    public static SafetyCheckResult reject(ToolErrorCategory category, String reason) { ... }
}
```

## Dependencies

| Component | Role |
|-----------|------|
| `ToolErrorCategory` | Classification for rejected operations |

## Backlinks

- Called by: [[services/agent-state-machine]] (TOOL_CALLING handler)
- Related: [[tool/tool-execution-recorder]], [[tool/tool-error-category]]
