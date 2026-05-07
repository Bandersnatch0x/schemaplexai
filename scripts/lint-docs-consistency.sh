#!/bin/bash
# lint-docs-consistency.sh — CI linter: check docs/ vs code consistency
# Usage: ./scripts/lint-docs-consistency.sh [--fix]
# Exit codes: 0=pass, 1=fail (HIGH), 2=warnings only (MEDIUM)

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/lib/common.sh"

PROJECT_ROOT=$(get_project_root)
cd "$PROJECT_ROOT"

FIX="${1:-}"
STAGED=0
STAGE_FILES=""

# Parse args
for arg in "$@"; do
    case "$arg" in
        --staged)
            STAGED=1
            STAGE_FILES=$(git diff --cached --name-only --diff-filter=ACM || true)
            ;;
    esac
done

ERRORS=0
WARNINGS=0

echo "=== Docs Consistency Lint ==="
echo ""

# --- Check 1: YAML frontmatter format ---
echo "[CHECK 1] YAML frontmatter format..."

DOCS_FILES=()
if [ "$STAGED" -eq 1 ] && [ -n "$STAGE_FILES" ]; then
    while IFS= read -r f; do
        [ -n "$f" ] || continue
        case "$(basename "$f")" in
            README.md|ADR-TEMPLATE.md) continue ;;
        esac
        if [[ "$f" =~ ^docs/(specs|plans|designs|decisions|standards)/.*\.md$ ]]; then
            DOCS_FILES+=("$f")
        fi
    done <<< "$STAGE_FILES"
else
    for f in docs/specs/*.md docs/plans/*.md docs/designs/*.md docs/decisions/*.md docs/standards/*.md; do
        [ -f "$f" ] || continue
        case "$(basename "$f")" in
            README.md|ADR-TEMPLATE.md) continue ;;
        esac
        DOCS_FILES+=("$f")
    done
fi

for f in "${DOCS_FILES[@]}"; do
    for field in topic stage version status; do
        if ! grep -q "^${field}:" "$f" 2>/dev/null; then
            echo "  HIGH: $(basename "$f") missing required field: $field"
            ERRORS=$((ERRORS + 1))
        fi
    done
done

if [ ${#DOCS_FILES[@]} -eq 0 ]; then
    echo "  No docs files to check."
fi

# --- Check 2: wiki/ AUTO-GENERATED markers ---
echo "[CHECK 2] wiki/ AUTO-GENERATED markers..."
for f in wiki/log.md wiki/active-areas.md wiki/decisions.md; do
    [ -f "$f" ] || continue
    if ! head -1 "$f" | grep -q "AUTO-GENERATED:"; then
        echo "  HIGH: $f missing AUTO-GENERATED marker (should be auto-generated)"
        ERRORS=$((ERRORS + 1))
    fi
done

# --- Check 3: DEVELOPMENT_STATUS.md marker ---
echo "[CHECK 3] DEVELOPMENT_STATUS.md marker..."
STATUS_FILE=".claude/DEVELOPMENT_STATUS.md"
if [ -f "$STATUS_FILE" ]; then
    if ! head -1 "$STATUS_FILE" | grep -q "AUTO-GENERATED:"; then
        echo "  HIGH: $STATUS_FILE missing AUTO-GENERATED marker"
        ERRORS=$((ERRORS + 1))
    fi
fi

# --- Check 4: Entity documentation coverage ---
echo "[CHECK 4] Entity documentation coverage..."
MISSING_ENTITIES=0
if [ -d "schemaplexai-model" ]; then
    for entity_file in $(find schemaplexai-model -name "*.java" -path "*/entity/*" 2>/dev/null); do
        class_name=$(basename "$entity_file" .java)
        # Skip base classes
        echo "$class_name" | grep -qi "^base" && continue
        wiki_file="wiki/entities/$(echo "$class_name" | sed 's/\([A-Z]\)/-\L\1/g' | sed 's/^-//' | tr '[:upper:]' '[:lower:]').md"
        if [ ! -f "$wiki_file" ]; then
            echo "  MEDIUM: Missing wiki page for entity: $class_name"
            MISSING_ENTITIES=$((MISSING_ENTITIES + 1))
        fi
    done
fi
WARNINGS=$((WARNINGS + MISSING_ENTITIES))

# --- Check 5: Controller documentation coverage ---
echo "[CHECK 5] Controller documentation coverage..."
MISSING_CTRL=0
if [ -d "schemaplexai-web" ]; then
    for ctrl_file in $(find schemaplexai-web -name "*Controller.java" 2>/dev/null); do
        class_name=$(basename "$ctrl_file" .java)
        # Skip base classes
        echo "$class_name" | grep -qi "^base" && continue
        wiki_file="wiki/controllers/$(echo "$class_name" | sed 's/\([A-Z]\)/-\L\1/g' | sed 's/^-//' | tr '[:upper:]' '[:lower:]').md"
        if [ ! -f "$wiki_file" ]; then
            echo "  MEDIUM: Missing wiki page for controller: $class_name"
            MISSING_CTRL=$((MISSING_CTRL + 1))
        fi
    done
fi
WARNINGS=$((WARNINGS + MISSING_CTRL))

# --- Summary ---
echo ""
echo "=== Summary ==="
echo "  HIGH issues:   $ERRORS"
echo "  MEDIUM issues: $WARNINGS"

if [ "$ERRORS" -gt 0 ]; then
    echo "  Result: FAIL"
    exit 1
elif [ "$WARNINGS" -gt 0 ]; then
    echo "  Result: WARN"
    exit 2
else
    echo "  Result: PASS"
    exit 0
fi
