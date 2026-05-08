#!/bin/bash
# stop-prompt-docs-review.sh — On session Stop, surface unsynced backend edits
# Called by Claude Code Stop hook
#
# Reads .claude/outputs/.docs-dirty and prints a yellow prompt listing the
# files that may need wiki/docs updates. Does not modify the dirty file —
# the pre-commit hook will clear it when the next commit succeeds.
#
# Always exits 0.

set +e

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
DIRTY_FILE="$PROJECT_ROOT/.claude/outputs/.docs-dirty"

[ ! -s "$DIRTY_FILE" ] && exit 0

COUNT=$(wc -l < "$DIRTY_FILE" 2>/dev/null | tr -d ' ')
[ -z "$COUNT" ] || [ "$COUNT" = "0" ] && exit 0

YELLOW="\033[33m"
BOLD="\033[1m"
RESET="\033[0m"

printf "${YELLOW}${BOLD}📝 Detected %s backend file edit(s) without confirmed wiki/docs sync:${RESET}\n" "$COUNT"
awk -F'\t' '{printf "   - %s\n", $2}' "$DIRTY_FILE"
printf "${YELLOW}→ Run: ${BOLD}bash scripts/sync-wiki.sh${RESET}${YELLOW} && ${BOLD}bash scripts/lint-docs-consistency.sh${RESET}\n"
printf "${YELLOW}  or commit will be blocked by pre-commit-wiki-sync hook.${RESET}\n"

exit 0
