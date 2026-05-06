#!/usr/bin/env bash
# Auto-screenshot hook: runs Playwright E2E screenshot suite if frontend files changed
set -euo pipefail

DIRTY_SENTINEL=".claude/outputs/.frontend-dirty"
SCREENSHOT_DIR=".claude/outputs/screenshots"

if [[ ! -f "$DIRTY_SENTINEL" ]]; then
  exit 0
fi

rm -f "$DIRTY_SENTINEL"

echo "[frontend-screenshots] Frontend changes detected — running visual verification..."

if ! command -v npx &> /dev/null; then
  echo "[frontend-screenshots] npx not found, skipping"
  exit 0
fi

if [[ ! -f "schemaplexai-ui/package.json" ]]; then
  echo "[frontend-screenshots] schemaplexai-ui/package.json not found, skipping"
  exit 0
fi

if ! grep -q "playwright" schemaplexai-ui/package.json 2>/dev/null; then
  echo "[frontend-screenshots] Playwright not installed in schemaplexai-ui, skipping"
  exit 0
fi

mkdir -p "$SCREENSHOT_DIR"

cd schemaplexai-ui

# Run the screenshot spec; if it fails, still report but don't block
if npx playwright test e2e/screenshots.spec.ts --reporter=line 2>&1; then
  echo "[frontend-screenshots] Screenshots captured in $SCREENSHOT_DIR"
else
  echo "[frontend-screenshots] Screenshot suite exited with errors (see above)"
fi
