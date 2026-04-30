#!/bin/bash
# change-init.sh — Initialize a new change workspace
# Usage: change-init.sh <feature-name>

set -euo pipefail

FEATURE_NAME="${1:-}"
if [ -z "$FEATURE_NAME" ]; then
    echo "Usage: change-init.sh <feature-name>"
    echo "Example: change-init.sh agent-execution-pause"
    exit 1
fi

# Normalize feature name (lowercase, hyphenated)
FEATURE_NAME=$(echo "$FEATURE_NAME" | tr '[:upper:]' '[:lower:]' | tr ' ' '-')

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
TEMPLATE_DIR="$PROJECT_ROOT/.claude/workflow/templates"
CHANGE_DIR="$PROJECT_ROOT/.claude/changes/$FEATURE_NAME"

if [ -d "$CHANGE_DIR" ]; then
    echo "Error: Change directory already exists: $CHANGE_DIR"
    exit 1
fi

mkdir -p "$CHANGE_DIR/notes"

# Copy templates
cp "$TEMPLATE_DIR/change-proposal.md" "$CHANGE_DIR/proposal.md"
cp "$TEMPLATE_DIR/change-spec.md" "$CHANGE_DIR/spec.md"
cp "$TEMPLATE_DIR/change-design.md" "$CHANGE_DIR/design.md"
cp "$TEMPLATE_DIR/change-tasks.md" "$CHANGE_DIR/tasks.md"

# Create context.md
DATE=$(date +%Y-%m-%d)
cat > "$CHANGE_DIR/context.md" <<EOF
---
change_id: $FEATURE_NAME
status: proposed
created_at: $DATE
---

# Task Context

## Change Goal

## Related Modules

## Key Files

## Decision Log

## Open Questions
EOF

# Replace placeholders in templates
AUTHOR=$(git config user.name 2>/dev/null || echo 'unknown')
for f in "$CHANGE_DIR"/*.md; do
    sed -i "s/{{feature-name}}/$FEATURE_NAME/g" "$f"
    sed -i "s/{{YYYY-MM-DD}}/$DATE/g" "$f"
    sed -i "s/{{Feature Title}}/$FEATURE_NAME/g" "$f"
    sed -i "s/{{author}}/$AUTHOR/g" "$f"
done

echo "Created change workspace: $CHANGE_DIR"
echo ""
echo "Next steps:"
echo "  1. Edit $CHANGE_DIR/proposal.md"
echo "  2. Start Claude Code session and say: 'Proceed with this proposal'"
echo ""
echo "Environment checks before coding:"
echo "  - Java 21: \$JAVA_HOME must point to JDK 21 (current: $(java -version 2>&1 | head -1 || echo 'not found'))"
echo "  - Backend: verify schemaplexai-dao and mybatis-plus-boot-starter in target module pom.xml"
echo "  - Frontend: cd schemaplexai-ui && npm run lint (if modifying UI code)"
echo ""
echo "Files created:"
ls -1 "$CHANGE_DIR"
