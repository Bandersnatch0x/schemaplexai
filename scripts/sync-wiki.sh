#!/bin/bash
# sync-wiki.sh — Generate wiki/ from docs/ status + git log
# Usage: ./scripts/sync-wiki.sh [--force]

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/lib/common.sh"

PROJECT_ROOT=$(get_project_root)
cd "$PROJECT_ROOT"

FORCE="${1:-}"
WIKI_DIR="$PROJECT_ROOT/wiki"
DOCS_DIR="$PROJECT_ROOT/docs"
STAMP_FILE="$PROJECT_ROOT/.wiki-sync-stamp"

echo "=== Syncing wiki/ from docs/ ==="

# --- 1. wiki/log.md ---
echo "  → Generating wiki/log.md..."
{
    auto_gen_marker
    echo ""
    echo "---"
    echo "title: Wiki Operation Log"
    echo "type: log"
    echo "source: auto-generated"
    echo "creation_date: $(date +%Y-%m-%d)"
    echo "update_date: $(date +%Y-%m-%d)"
    echo "tags: [wiki, log, maintenance]"
    echo "confidence: high"
    echo "---"
    echo ""
    echo "# Wiki Operation Log"
    echo ""
    echo "> Auto-generated from git log + docs/ status. Manual edits will be overwritten."
    echo ""

    # Recent 20 commits with dates
    git log --oneline --date=short --format="## %ad — %s%n%h" -20

    echo ""
    echo "---"
    echo ""
    echo "## Recent Docs Status Changes"
    echo ""
    # List docs files with their current status
    for f in "$DOCS_DIR"/specs/*.md "$DOCS_DIR"/plans/*.md "$DOCS_DIR"/designs/*.md; do
        [ -f "$f" ] || continue
        topic=$(parse_yaml_field "$f" "topic")
        status=$(parse_yaml_field "$f" "status")
        stage=$(parse_yaml_field "$f" "stage")
        version=$(parse_yaml_field "$f" "version")
        [ -n "$topic" ] && echo "- **$topic** ($stage $version): $status — $(basename "$f")"
    done
} > "$WIKI_DIR/log.md"

# --- 2. wiki/active-areas.md ---
echo "  → Generating wiki/active-areas.md..."
{
    auto_gen_marker
    echo ""
    echo "---"
    echo "title: Active Development Areas"
    echo "type: index"
    echo "source: auto-generated"
    echo "creation_date: $(date +%Y-%m-%d)"
    echo "update_date: $(date +%Y-%m-%d)"
    echo "tags: [active, development]"
    echo "confidence: high"
    echo "---"
    echo ""
    echo "# Active Development Areas"
    echo ""
    echo "> Auto-generated from docs/ status=进行中/评审中 entries."
    echo ""

    echo "## Active Specs"
    echo ""
    for f in "$DOCS_DIR"/specs/*.md; do
        [ -f "$f" ] || continue
        status=$(parse_yaml_field "$f" "status")
        if [ "$status" = "进行中" ] || [ "$status" = "评审中" ]; then
            topic=$(parse_yaml_field "$f" "topic")
            stage=$(parse_yaml_field "$f" "stage")
            echo "- **$topic** ($stage) — $status — $(basename "$f")"
        fi
    done

    echo ""
    echo "## Active Plans"
    echo ""
    for f in "$DOCS_DIR"/plans/*.md; do
        [ -f "$f" ] || continue
        status=$(parse_yaml_field "$f" "status")
        if [ "$status" = "进行中" ] || [ "$status" = "评审中" ]; then
            topic=$(parse_yaml_field "$f" "topic")
            stage=$(parse_yaml_field "$f" "stage")
            echo "- **$topic** ($stage) — $status — $(basename "$f")"
        fi
    done

    echo ""
    echo "## Active Changes (.claude/changes/)"
    echo ""
    if [ -d "$PROJECT_ROOT/.claude/changes" ]; then
        for change_dir in "$PROJECT_ROOT/.claude/changes"/*/; do
            [ -d "$change_dir" ] || continue
            name=$(basename "$change_dir")
            [ "$name" = "archive" ] && continue
            has_spec="no"
            has_tasks="no"
            [ -f "$change_dir/spec.md" ] && has_spec="yes"
            [ -f "$change_dir/tasks.md" ] && has_tasks="yes"
            echo "- **$name**: spec=$has_spec, tasks=$has_tasks"
        done
    fi
} > "$WIKI_DIR/active-areas.md"

# --- 3. wiki/decisions.md ---
echo "  → Generating wiki/decisions.md..."
{
    auto_gen_marker
    echo ""
    echo "---"
    echo "title: Architecture Decision Records Index"
    echo "type: index"
    echo "source: auto-generated"
    echo "creation_date: $(date +%Y-%m-%d)"
    echo "update_date: $(date +%Y-%m-%d)"
    echo "tags: [decisions, adr]"
    echo "confidence: high"
    echo "---"
    echo ""
    echo "# Architecture Decision Records"
    echo ""
    echo "> Auto-generated from docs/decisions/ ADR files."
    echo ""

    for f in "$DOCS_DIR"/decisions/ADR-*.md; do
        [ -f "$f" ] || continue
        topic=$(parse_yaml_field "$f" "topic")
        status=$(parse_yaml_field "$f" "status")
        version=$(parse_yaml_field "$f" "version")
        fname=$(basename "$f")
        echo "- [$topic]($f) ($version) — $status"
    done
} > "$WIKI_DIR/decisions.md"

# --- 4. wiki/gaps.md (preserve manual content, append auto section) ---
echo "  → Updating wiki/gaps.md auto section..."
GAPS_MANUAL=""
if [ -f "$WIKI_DIR/gaps.md" ]; then
    # Extract content before AUTO-GENERATED marker (if exists)
    if grep -q "AUTO-GENERATED:" "$WIKI_DIR/gaps.md"; then
        GAPS_MANUAL=$(sed '/AUTO-GENERATED:/q' "$WIKI_DIR/gaps.md" | head -n -1)
    else
        GAPS_MANUAL=$(cat "$WIKI_DIR/gaps.md")
    fi
fi

{
    [ -n "$GAPS_MANUAL" ] && echo "$GAPS_MANUAL"
    echo ""
    echo "## Auto-Generated Gap Scan"
    echo ""
    auto_gen_marker
    echo ""

    echo "### Undocumented Entities"
    echo ""
    # Find Java entity files not in wiki/entities/
    if [ -d "$PROJECT_ROOT/schemaplexai-model" ]; then
        for entity_file in $(find "$PROJECT_ROOT/schemaplexai-model" -name "*.java" -path "*/entity/*" 2>/dev/null); do
            class_name=$(basename "$entity_file" .java)
            wiki_file="$WIKI_DIR/entities/$(echo "$class_name" | sed 's/\([A-Z]\)/-\L\1/g' | sed 's/^-//' | tr '[:upper:]' '[:lower:]').md"
            if [ ! -f "$wiki_file" ]; then
                echo "- Missing wiki page for entity: \`$class_name\` (source: $entity_file)"
            fi
        done
    fi

    echo ""
    echo "### Undocumented Controllers"
    echo ""
    if [ -d "$PROJECT_ROOT/schemaplexai-web" ]; then
        for ctrl_file in $(find "$PROJECT_ROOT/schemaplexai-web" -name "*Controller.java" 2>/dev/null); do
            class_name=$(basename "$ctrl_file" .java)
            wiki_file="$WIKI_DIR/controllers/$(echo "$class_name" | sed 's/\([A-Z]\)/-\L\1/g' | sed 's/^-//' | tr '[:upper:]' '[:lower:]').md"
            if [ ! -f "$wiki_file" ]; then
                echo "- Missing wiki page for controller: \`$class_name\` (source: $ctrl_file)"
            fi
        done
    fi

    echo ""
    echo "### Undocumented Services"
    echo ""
    if [ -d "$PROJECT_ROOT/schemaplexai-web" ] || [ -d "$PROJECT_ROOT/schemaplexai-agent-engine" ]; then
        for svc_file in $(find "$PROJECT_ROOT" -name "*Service.java" -not -path "*/test/*" -not -name "*Test.java" 2>/dev/null); do
            class_name=$(basename "$svc_file" .java)
            # Skip interfaces without Impl
            [[ "$class_name" == *Impl ]] && continue
            wiki_file="$WIKI_DIR/services/$(echo "$class_name" | sed 's/\([A-Z]\)/-\L\1/g' | sed 's/^-//' | tr '[:upper:]' '[:lower:]').md"
            if [ ! -f "$wiki_file" ]; then
                echo "- Missing wiki page for service: \`$class_name\` (source: $svc_file)"
            fi
        done
    fi
} > "$WIKI_DIR/gaps.md"

# --- 5. Write sync stamp ---
date -u +%Y-%m-%dT%H:%M:%SZ > "$STAMP_FILE"

echo ""
echo "=== wiki/ sync complete ==="
echo "Stamp written to $STAMP_FILE"
