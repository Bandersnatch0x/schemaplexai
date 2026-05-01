#!/bin/bash
# stop-gen-status.sh — Stop hook: regenerate DEVELOPMENT_STATUS.md on session end

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

echo "=== Stop hook: regenerating DEVELOPMENT_STATUS.md ==="
bash "$PROJECT_ROOT/scripts/gen-dev-status.sh"
echo "=== Stop hook complete ==="
