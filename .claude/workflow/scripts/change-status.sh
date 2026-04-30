#!/bin/bash
# change-status.sh — Show all active changes status
# Usage: change-status.sh

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
CHANGES_DIR="$PROJECT_ROOT/.claude/changes"

if [ ! -d "$CHANGES_DIR" ]; then
    echo "No changes directory found."
    exit 0
fi

echo "========================================"
echo "  Active Changes in .claude/changes/"
echo "========================================"
echo ""

found_any=0
for change_dir in "$CHANGES_DIR"/*; do
    [ -d "$change_dir" ] || continue
    [ "$(basename "$change_dir")" = "archive" ] && continue

    found_any=1
    name=$(basename "$change_dir")

    has_proposal="[ ]"
    has_spec="[ ]"
    has_design="[ ]"
    has_tasks="[ ]"
    [ -f "$change_dir/proposal.md" ] && has_proposal="[x]"
    [ -f "$change_dir/spec.md" ] && has_spec="[x]"
    [ -f "$change_dir/design.md" ] && has_design="[x]"
    [ -f "$change_dir/tasks.md" ] && has_tasks="[x]"

    status="unknown"
    if [ -f "$change_dir/proposal.md" ]; then
        status=$(grep -m1 "^status:" "$change_dir/proposal.md" | cut -d: -f2 | tr -d ' ' || echo "unknown")
    fi

    completed=0
    total=0
    if [ -f "$change_dir/tasks.md" ]; then
        completed=$(grep -c "completed" "$change_dir/tasks.md" 2>/dev/null || echo 0)
        total=$(grep -c "^### Task" "$change_dir/tasks.md" 2>/dev/null || echo 0)
    fi

    echo "[$name]"
    echo "  Status : $status"
    echo "  Phases : Proposal $has_proposal | Spec $has_spec | Design $has_design | Tasks $has_tasks"
    if [ "$total" -gt 0 ]; then
        echo "  Tasks  : $completed / $total completed"
    fi
    echo ""
done

if [ "$found_any" -eq 0 ]; then
    echo "No active changes found."
    echo ""
    echo "To create a new change:"
    echo "  .claude/workflow/scripts/change-init.sh <feature-name>"
fi
