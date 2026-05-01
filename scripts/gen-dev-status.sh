#!/bin/bash
# gen-dev-status.sh — Generate .claude/DEVELOPMENT_STATUS.md from current state
# Usage: ./scripts/gen-dev-status.sh

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/lib/common.sh"

PROJECT_ROOT=$(get_project_root)
cd "$PROJECT_ROOT"

STATUS_FILE="$PROJECT_ROOT/.claude/DEVELOPMENT_STATUS.md"
CHANGES_DIR="$PROJECT_ROOT/.claude/changes"
DOCS_DIR="$PROJECT_ROOT/docs"

echo "=== Generating DEVELOPMENT_STATUS.md ==="

{
    auto_gen_marker "gen-dev-status.sh"
    echo ""
    echo "# Development Status — $(date +%Y-%m-%d)"
    echo ""

    # --- This Week: from docs/ status=已批准 ---
    echo "## This Week (Completed)"
    echo ""
    found=0
    # Recent approved specs
    for f in "$DOCS_DIR"/specs/*.md "$DOCS_DIR"/plans/*.md "$DOCS_DIR"/designs/*.md; do
        [ -f "$f" ] || continue
        status=$(parse_yaml_field "$f" "status")
        topic=$(parse_yaml_field "$f" "topic")
        stage=$(parse_yaml_field "$f" "stage")
        if [ "$status" = "已批准" ]; then
            # Check if approved recently (within 7 days)
            mtime=$(stat -c %Y "$f" 2>/dev/null || stat -f %m "$f" 2>/dev/null || echo 0)
            week_ago=$(date -d '7 days ago' +%s 2>/dev/null || date -v-7d +%s 2>/dev/null || echo 0)
            if [ "$mtime" -ge "$week_ago" ]; then
                echo "- [x] $topic ($stage) — $(basename "$f")"
                found=1
            fi
        fi
    done
    [ "$found" -eq 0 ] && echo "- (none this week)"
    echo ""

    # --- Active Changes ---
    echo "## Active Changes"
    echo ""
    if [ -d "$CHANGES_DIR" ]; then
        echo "| Change | Phases | Status |"
        echo "|--------|--------|--------|"
        found=0
        for change_dir in "$CHANGES_DIR"/*/; do
            [ -d "$change_dir" ] || continue
            name=$(basename "$change_dir")
            [ "$name" = "archive" ] && continue

            phases=""
            [ -f "$change_dir/proposal.md" ] && phases="${phases}Propose " || phases="${phases}- "
            [ -f "$change_dir/spec.md" ] && phases="${phases}Spec " || phases="${phases}- "
            [ -f "$change_dir/design.md" ] && phases="${phases}Design " || phases="${phases}- "
            [ -f "$change_dir/tasks.md" ] && phases="${phases}Plan " || phases="${phases}- "

            status="unknown"
            if [ -f "$change_dir/proposal.md" ]; then
                status=$(parse_yaml_field "$change_dir/proposal.md" "status")
            fi

            echo "| $name | $phases | $status |"
            found=1
        done
        [ "$found" -eq 0 ] && echo "| (none) | | |"
    else
        echo "| (none) | | |"
    fi
    echo ""

    # --- Recent Decisions ---
    echo "## Recent Decisions"
    echo ""
    found=0
    for f in "$DOCS_DIR"/decisions/ADR-*.md; do
        [ -f "$f" ] || continue
        topic=$(parse_yaml_field "$f" "topic")
        status=$(parse_yaml_field "$f" "status")
        mtime=$(stat -c %Y "$f" 2>/dev/null || stat -f %m "$f" 2>/dev/null || echo 0)
        week_ago=$(date -d '7 days ago' +%s 2>/dev/null || date -v-7d +%s 2>/dev/null || echo 0)
        if [ "$mtime" -ge "$week_ago" ]; then
            fname=$(basename "$f")
            echo "- **$topic** ($status) — $fname"
            found=1
        fi
    done
    [ "$found" -eq 0 ] && echo "- (none this week)"
    echo ""

    # --- Links ---
    echo "## Links"
    echo ""
    echo "- [Specs](docs/specs/) | [Plans](docs/plans/) | [Designs](docs/designs/)"
    echo "- [Wiki Index](wiki/index.md) | [Change Log](wiki/log.md) | [Active Areas](wiki/active-areas.md)"
    echo "- [Decisions](docs/decisions/) | [Standards](docs/standards/)"
} > "$STATUS_FILE"

echo "  → Written to $STATUS_FILE"
echo "=== Done ==="
