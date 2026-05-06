#!/bin/bash
# setup-workflow-loop.sh — Initialize workflow-adopter state (8 phases)
# Usage: setup-workflow-loop.sh <feature-name> <description...>

set -euo pipefail

FEATURE_NAME="${1:-}"
shift || true
DESCRIPTION="$*"

if [ -z "$FEATURE_NAME" ]; then
  echo "Error: No feature name provided" >&2
  echo "" >&2
  echo "Usage: /workflow-loop <feature-name> <description>" >&2
  echo "Example: /workflow-loop agent-pause-resume Add pause/resume to agent execution" >&2
  exit 1
fi

# Normalize feature name
FEATURE_NAME=$(echo "$FEATURE_NAME" | tr '[:upper:]' '[:lower:]' | tr ' ' '-')

if [ -z "$DESCRIPTION" ]; then
  DESCRIPTION="$FEATURE_NAME"
fi

mkdir -p .claude

cat > .claude/workflow-loop.local.md <<EOF
---
active: true
feature_name: $FEATURE_NAME
description: $DESCRIPTION
current_phase: propose
iteration: 1
max_iterations: 20
started_at: "$(date -u +%Y-%m-%dT%H:%M:%SZ)"
phases:
  propose:
    status: pending
    gate_result: null
    notes: ""
  review:
    status: pending
    gate_result: null
    notes: ""
    verdict: null
  spec:
    status: pending
    gate_result: null
    notes: ""
  design:
    status: pending
    gate_result: null
    notes: ""
  plan:
    status: pending
    gate_result: null
    notes: ""
  apply:
    status: pending
    gate_result: null
    notes: ""
    files_changed: []
    tests_passing: false
  deliver:
    status: pending
    gate_result: null
    notes: ""
  archive:
    status: pending
    gate_result: null
    notes: ""
---

# Workflow Loop: $FEATURE_NAME

$DESCRIPTION
EOF

echo "Workflow loop initialized!"
echo ""
echo "Feature: $FEATURE_NAME"
echo "Description: $DESCRIPTION"
echo "Max iterations: 20"
echo ""
echo "The 8-phase lifecycle will execute:"
echo "  Propose → Review (圆桌) → Spec → Design → Plan → Apply → Deliver → Archive"
echo ""
echo "Review phase uses 4-expert roundtable (产品/架构/安全/AI工程)"
echo ""
echo "To monitor: head -10 .claude/workflow-loop.local.md"
echo "To cancel: /cancel-workflow"
