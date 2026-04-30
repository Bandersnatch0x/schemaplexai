#!/bin/bash
# change-archive.sh — Archive a completed change
# Usage: change-archive.sh <feature-name>

set -euo pipefail

FEATURE_NAME="${1:-}"
if [ -z "$FEATURE_NAME" ]; then
    echo "Usage: change-archive.sh <feature-name>"
    echo "Example: change-archive.sh agent-execution-pause"
    exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
CHANGES_DIR="$PROJECT_ROOT/.claude/changes"
ARCHIVE_DIR="$CHANGES_DIR/archive"
SOURCE_DIR="$CHANGES_DIR/$FEATURE_NAME"

if [ ! -d "$SOURCE_DIR" ]; then
    echo "Error: Change not found: $SOURCE_DIR"
    echo "Active changes:"
    ls -1 "$CHANGES_DIR" 2>/dev/null || true
    exit 1
fi

DATE=$(date +%Y-%m-%d)
DEST_DIR="$ARCHIVE_DIR/${FEATURE_NAME}-${DATE}"

if [ -d "$DEST_DIR" ]; then
    count=1
    while [ -d "${DEST_DIR}-v${count}" ]; do
        count=$((count + 1))
    done
    DEST_DIR="${DEST_DIR}-v${count}"
fi

mkdir -p "$ARCHIVE_DIR"
mv "$SOURCE_DIR" "$DEST_DIR"

echo "Archived change: $FEATURE_NAME"
echo "Source: $SOURCE_DIR"
echo "Dest:   $DEST_DIR"
echo ""
echo "Reminder:"
echo "  1. Update docs/specs/ if public contract changed"
echo "  2. Update docs/designs/ if architecture changed"
echo "  3. Update wiki/log.md with learnings"
echo "  4. Update wiki/gaps.md if gaps found"
