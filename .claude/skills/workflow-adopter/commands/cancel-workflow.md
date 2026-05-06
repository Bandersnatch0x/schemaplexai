---
description: "Cancel active workflow-adopter loop"
hide-from-slash-command-tool: "true"
---

# Cancel Workflow

To cancel the active workflow-adopter loop:

1. Check if `.claude/workflow-loop.local.md` exists
2. **If not found**: Report "No active workflow loop found."
3. **If found**:
   - Read `.claude/workflow-loop.local.md` to get the current phase and iteration
   - Report the phase that was in progress and how many iterations ran
   - Remove the file: `rm .claude/workflow-loop.local.md`
   - Report: "Cancelled workflow loop (was at phase X, iteration N)"
   - Do NOT clean up `.claude/changes/<feature>/` — the change workspace remains for manual continuation
