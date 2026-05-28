#!/bin/bash
# post-edit-mark-docs-dirty.sh — Mark backend code edits for doc-sync review
# Called by Claude Code PostToolUse hook (Edit|Write)
#
# Reads $TOOL_INPUT JSON on stdin, extracts file_path. If the path looks like
# a backend Java source under controller/entity/service/mapper, append it to
# .claude/outputs/.docs-dirty so the Stop hook can prompt and the next commit
# can verify wiki/docs are in sync.
#
# Always exits 0 — never blocks Edit/Write.

set +e

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
OUTPUT_DIR="$PROJECT_ROOT/.claude/outputs"
DIRTY_FILE="$OUTPUT_DIR/.docs-dirty"

mkdir -p "$OUTPUT_DIR" 2>/dev/null

# Extract file_path from $TOOL_INPUT JSON. Try jq first, fall back to grep.
FILE_PATH=""
if command -v jq >/dev/null 2>&1; then
    FILE_PATH=$(echo "${TOOL_INPUT:-}" | jq -r '.file_path // empty' 2>/dev/null)
fi
if [ -z "$FILE_PATH" ]; then
    FILE_PATH=$(echo "${TOOL_INPUT:-}" | grep -oE '"file_path"[[:space:]]*:[[:space:]]*"[^"]+"' | head -1 | sed -E 's/.*"file_path"[[:space:]]*:[[:space:]]*"([^"]+)".*/\1/')
fi

[ -z "$FILE_PATH" ] && exit 0

# Match backend Java keys: controller / entity / service / mapper
# Path shape: schemaplexai-*/src/main/java/.../{controller,entity,service,mapper}/*.java
if echo "$FILE_PATH" | grep -qE '/schemaplexai-[^/]+/src/main/java/.*/(controller|entity|service|mapper)/[^/]+\.java$'; then
    REL=$(echo "$FILE_PATH" | sed "s|$PROJECT_ROOT/||")
    TS=$(date -u +%Y-%m-%dT%H:%M:%SZ)
    # Avoid duplicate lines; append only if not already recorded for this session
    if [ ! -f "$DIRTY_FILE" ] || ! grep -qF "$REL" "$DIRTY_FILE" 2>/dev/null; then
        echo "$TS	$REL" >> "$DIRTY_FILE"
    fi
fi

exit 0
