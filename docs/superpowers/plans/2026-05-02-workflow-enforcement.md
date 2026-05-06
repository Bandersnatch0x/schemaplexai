# 工作流强约束闭环实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现 docs/ → wiki/ → DEVELOPMENT_STATUS.md 三体系强约束联动，用机械 hook 替代"靠 Claude 自觉"。

**Architecture:** docs/ 是唯一写入层（SSOT），wiki/ 由脚本自动生成（只读派生视图），DEVELOPMENT_STATUS.md 由 Stop hook 自动生成。PreCommit hook 强制同步，CI linter 校验一致性，LLM-as-Judge 门禁集成到 workflow-adopter。

**Tech Stack:** Bash scripts, Claude Code hooks (settings.local.json), YAML frontmatter parsing, git, workflow-adopter SKILL.md

---

## Phase 0: 基础设施

### Task 0.1: 创建 scripts 目录和 hooks 目录

**Files:**
- Create: `scripts/` (项目根目录)
- Create: `.claude/hooks/` (已存在但为空)

- [ ] **Step 1: 创建目录结构**

```bash
cd D:/code_space/frige
mkdir -p scripts/lib
mkdir -p .claude/hooks
```

- [ ] **Step 2: 验证目录已创建**

```bash
ls -la scripts/
ls -la .claude/hooks/
```

Expected: 两个目录存在且为空

- [ ] **Step 3: Commit**

```bash
git add scripts/ .claude/hooks/
git commit -m "chore: create scripts and hooks directories for workflow enforcement"
```

---

### Task 0.2: 更新 wiki-constraints.md — wiki/ 改为只读自动生成

**Files:**
- Modify: `docs/standards/2026-04-30-v1.0-wiki-constraints.md`

- [ ] **Step 1: 读取当前文件，定位需要修改的部分**

Read: `docs/standards/2026-04-30-v1.0-wiki-constraints.md`

需要修改的部分：
- 第 1 节 "定位与边界" 表格中 wiki/ 的权威性描述
- 新增第 8 节 "自动生成规则"
- 更新第 7 节 "质量检查"

- [ ] **Step 2: 修改第 1 节定位表格**

将 wiki/ 权威性从 "中（持续更新，允许滞后）" 改为 "派生视图（自动生成，禁止手动编辑）"。

在 wiki-constraints.md 的第 21-30 行区域，替换表格：

```markdown
| 维度 | `docs/` | `wiki/` |
|------|---------|---------|
| 读者 | 全团队 | AI Agent / 开发者快速查询 |
| 权威性 | 高（评审后冻结） | 派生视图（自动生成，禁止手动编辑） |
| 格式 | 标准 SDD 文档（YAML front-matter + 章节） | 独立 YAML front-matter + 知识片段 |
| 生命周期 | 创建 → 评审 → 批准 → 归档 | 脚本生成，随 docs/ 变更自动更新 |
| 内容 | 规格、设计、计划、决策 | 代码洞察、数据模型、API 速查、已知问题 |
| 写入权限 | 人工/AI 可写 | 只读（`scripts/sync-wiki.sh` 生成） |
```

- [ ] **Step 3: 修改第 4 节更新触发条件**

在第 83-92 行区域，添加说明：这些触发条件现在由 `scripts/sync-wiki.sh` 自动执行，不再依赖手动操作。

在表格前添加一行说明：
```markdown
> 以下触发条件由 `scripts/sync-wiki.sh` 自动执行。手动编辑 wiki/ 文件已被 PreCommit hook 禁止。
```

- [ ] **Step 4: 新增第 8 节自动生成规则**

在文件末尾（第 120 行之后）添加：

```markdown
## 8. 自动生成规则（Poka-Yoke）

wiki/ 所有文件由 `scripts/sync-wiki.sh` 自动生成，遵循以下规则：

### 8.1 自动生成标记

所有自动生成的文件必须以标记开头：
```
<!-- AUTO-GENERATED: sync-wiki.sh at YYYY-MM-DDTHH:MM:SSZ -->
```

### 8.2 写入禁止

- PreCommit hook 检测到 wiki/ 文件被手动编辑（无 AUTO-GENERATED 标记）时，**拒绝提交**
- 错误提示：`wiki/ 是只读派生视图。请修改 docs/ 后运行 scripts/sync-wiki.sh`

### 8.3 同步触发

| 触发时机 | 行为 |
|---------|------|
| git commit 且 docs/ 有变更 | PreCommit hook 调用 sync-wiki.sh |
| 手动调用 | `./scripts/sync-wiki.sh` |
| CI 定期 | doc-gardening agent 全量校验 |

### 8.4 可同步文件列表

| wiki/ 目标文件 | 数据源 | 更新频率 |
|---------------|--------|---------|
| `log.md` | git log + docs/ YAML status | 每次 docs/ 变更 |
| `active-areas.md` | docs/specs/ + docs/plans/ status=进行中 | 每次 docs/ 变更 |
| `gaps.md` | 代码扫描 vs wiki/ 现有文档 | 每次 docs/ 变更 |
| `decisions.md` | docs/decisions/ ADR 索引 | 每次 docs/ 变更 |
| `technical-debt.md` | docs/ + code TODO/FIXME | 每周 (doc-gardening) |
```

- [ ] **Step 5: 更新文件版本号**

将文件开头 YAML frontmatter 的 version 改为 `v1.1`，status 改为 `已批准`，supersedes 改为 `v1.0`。

- [ ] **Step 6: Commit**

```bash
git add docs/standards/2026-04-30-v1.0-wiki-constraints.md
git commit -m "docs: update wiki-constraints — wiki/ becomes read-only auto-generated view"
```

---

## Phase 1 (P0): sync-wiki.sh + PreCommit hook

### Task 1.1: 创建 sync-wiki.sh 核心脚本

**Files:**
- Create: `scripts/sync-wiki.sh`
- Create: `scripts/lib/common.sh` (共享函数)

- [ ] **Step 1: 创建共享函数库**

Write: `scripts/lib/common.sh`

```bash
#!/bin/bash
# common.sh — Shared functions for workflow scripts

set -euo pipefail

# Get project root (works from any subdirectory)
get_project_root() {
    git rev-parse --show-toplevel
}

# Generate AUTO-GENERATED marker
auto_gen_marker() {
    echo "<!-- AUTO-GENERATED: sync-wiki.sh at $(date -u +%Y-%m-%dT%H:%M:%SZ) -->"
}

# Parse YAML frontmatter field value from a file
# Usage: parse_yaml_field "file.md" "status"
parse_yaml_field() {
    local file="$1"
    local field="$2"
    if [ ! -f "$file" ]; then
        echo ""
        return
    fi
    # Extract value between --- blocks
    sed -n '/^---$/,/^---$/p' "$file" | grep "^${field}:" | head -1 | cut -d: -f2- | sed 's/^ *//;s/ *$//'
}

# List files with a specific YAML status in a directory
# Usage: list_files_by_status "docs/specs/" "已批准"
list_files_by_status() {
    local dir="$1"
    local target_status="$2"
    for f in "$dir"*.md; do
        [ -f "$f" ] || continue
        local s
        s=$(parse_yaml_field "$f" "status")
        if [ "$s" = "$target_status" ]; then
            echo "$f"
        fi
    done
}

# Write file with auto-generated marker
# Usage: write_auto_gen "wiki/log.md" "content here"
write_auto_gen() {
    local file="$1"
    local content="$2"
    local marker
    marker=$(auto_gen_marker)
    mkdir -p "$(dirname "$file")"
    printf "%s\n\n%s\n" "$marker" "$content" > "$file"
}

# Check if file has AUTO-GENERATED marker
is_auto_generated() {
    local file="$1"
    [ -f "$file" ] && head -1 "$file" | grep -q "AUTO-GENERATED:"
}
```

- [ ] **Step 2: 创建 sync-wiki.sh 主脚本**

Write: `scripts/sync-wiki.sh`

```bash
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
            echo "$class_name" | grep -q "Impl$" && continue
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
```

- [ ] **Step 3: 设置执行权限并测试**

```bash
chmod +x scripts/sync-wiki.sh
chmod +x scripts/lib/common.sh
./scripts/sync-wiki.sh
```

Expected: 输出 "=== wiki/ sync complete ==="，wiki/ 目录下 log.md / active-areas.md / decisions.md / gaps.md 已更新且以 AUTO-GENERATED 标记开头。

- [ ] **Step 4: 验证输出格式**

```bash
head -3 wiki/log.md
head -3 wiki/active-areas.md
head -3 wiki/decisions.md
grep -c "AUTO-GENERATED" wiki/log.md wiki/active-areas.md wiki/decisions.md wiki/gaps.md
```

Expected: 每个文件第一行包含 `<!-- AUTO-GENERATED: sync-wiki.sh at`，计数均为 1。

- [ ] **Step 5: Commit**

```bash
git add scripts/sync-wiki.sh scripts/lib/common.sh
git commit -m "feat: add sync-wiki.sh — auto-generate wiki/ from docs/ status + git log"
```

---

### Task 1.2: 创建 PreCommit hook 脚本

**Files:**
- Create: `.claude/hooks/pre-commit-wiki-sync.sh`

- [ ] **Step 1: 创建 hook 脚本**

Write: `.claude/hooks/pre-commit-wiki-sync.sh`

```bash
#!/bin/bash
# pre-commit-wiki-sync.sh — Enforce wiki/ sync on docs/ changes
# Called by Claude Code PreToolUse hook (Bash tool with git commit)

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
STAMP_FILE="$PROJECT_ROOT/.wiki-sync-stamp"
SYNC_SCRIPT="$PROJECT_ROOT/scripts/sync-wiki.sh"

cd "$PROJECT_ROOT"

# Get staged files
STAGED_FILES=$(git diff --cached --name-only 2>/dev/null || echo "")

if [ -z "$STAGED_FILES" ]; then
    exit 0
fi

ERRORS=""

# Rule 1: If docs/ changed, wiki/ must be synced
DOCS_CHANGED=$(echo "$STAGED_FILES" | grep "^docs/" || true)
if [ -n "$DOCS_CHANGED" ]; then
    if [ ! -f "$STAMP_FILE" ]; then
        ERRORS="${ERRORS}ERROR: docs/ files changed but wiki/ never synced.\n  Run: ./scripts/sync-wiki.sh\n\n"
    else
        STAMP_TIME=$(cat "$STAMP_FILE")
        # Check if any docs/ file is newer than stamp
        for f in $DOCS_CHANGED; do
            if [ -f "$f" ] && [ "$f" -nt "$STAMP_FILE" ]; then
                ERRORS="${ERRORS}ERROR: $f changed after last wiki sync ($STAMP_TIME).\n  Run: ./scripts/sync-wiki.sh\n\n"
                break
            fi
        done
    fi
fi

# Rule 2: wiki/ files must not be manually edited (unless auto-generated)
WIKI_CHANGED=$(echo "$STAGED_FILES" | grep "^wiki/" || true)
if [ -n "$WIKI_CHANGED" ]; then
    for f in $WIKI_CHANGED; do
        if [ -f "$f" ]; then
            if ! head -1 "$f" | grep -q "AUTO-GENERATED:"; then
                ERRORS="${ERRORS}ERROR: $f is manually edited (no AUTO-GENERATED marker).\n  wiki/ is a read-only derived view. Modify docs/ and run ./scripts/sync-wiki.sh\n\n"
            fi
        fi
    done
fi

# Rule 3: DEVELOPMENT_STATUS.md must not be manually edited
STATUS_CHANGED=$(echo "$STAGED_FILES" | grep "\.claude/DEVELOPMENT_STATUS\.md$" || true)
if [ -n "$STATUS_CHANGED" ]; then
    STATUS_FILE="$PROJECT_ROOT/.claude/DEVELOPMENT_STATUS.md"
    if [ -f "$STATUS_FILE" ] && ! head -1 "$STATUS_FILE" | grep -q "AUTO-GENERATED:"; then
        ERRORS="${ERRORS}ERROR: .claude/DEVELOPMENT_STATUS.md is manually edited.\n  This file is auto-generated by Stop hook. Revert changes.\n\n"
    fi
fi

# Report errors
if [ -n "$ERRORS" ]; then
    echo ""
    echo "=========================================="
    echo "  Wiki Sync Enforcement — BLOCKED"
    echo "=========================================="
    echo ""
    echo -e "$ERRORS"
    exit 1
fi

exit 0
```

- [ ] **Step 2: 设置执行权限并手动测试**

```bash
chmod +x .claude/hooks/pre-commit-wiki-sync.sh

# Test: simulate docs/ change without sync
echo "# test" >> docs/specs/test-temp.md
git add docs/specs/test-temp.md
bash .claude/hooks/pre-commit-wiki-sync.sh
# Expected: ERROR output, exit 1

# Cleanup
git reset HEAD docs/specs/test-temp.md
rm docs/specs/test-temp.md
```

- [ ] **Step 3: 测试 wiki/ 手动编辑检测**

```bash
echo "# manual edit" >> wiki/log.md
git add wiki/log.md
bash .claude/hooks/pre-commit-wiki-sync.sh
# Expected: ERROR about manual edit, exit 1

# Cleanup
git reset HEAD wiki/log.md
git checkout wiki/log.md
```

- [ ] **Step 4: 测试正向路径 — 同步后提交应通过**

```bash
./scripts/sync-wiki.sh
bash .claude/hooks/pre-commit-wiki-sync.sh
# Expected: exit 0 (no errors)
```

- [ ] **Step 5: Commit**

```bash
git add .claude/hooks/pre-commit-wiki-sync.sh
git commit -m "feat: add PreCommit hook — enforce wiki/ sync, block manual edits"
```

---

### Task 1.3: 注册 PreCommit hook 到 settings.local.json

**Files:**
- Modify: `.claude/settings.local.json`

- [ ] **Step 1: 读取当前 settings.local.json**

Read: `.claude/settings.local.json`

当前 hooks 配置（第 103-114 行）只有 SessionStart hook。需要添加 PreToolUse hook。

- [ ] **Step 2: 添加 PreToolUse hook**

在 `hooks` 对象中添加 `PreToolUse` 数组。在现有 `SessionStart` 之后添加：

```json
"PreToolUse": [
  {
    "matcher": "Bash",
    "hooks": [
      {
        "type": "command",
        "command": "echo \"$TOOL_INPUT\" | grep -q 'git commit' && bash .claude/hooks/pre-commit-wiki-sync.sh || true"
      }
    ]
  }
]
```

注意：此 hook 仅在 Bash 工具调用包含 `git commit` 时触发。`|| true` 确保非 git commit 调用不会阻塞。

- [ ] **Step 3: 验证 JSON 格式正确**

```bash
python3 -c "import json; json.load(open('.claude/settings.local.json'))"
# Expected: no output (valid JSON)
```

- [ ] **Step 4: Commit**

```bash
git add .claude/settings.local.json
git commit -m "chore: register PreToolUse hook for wiki sync enforcement"
```

---

## Phase 2 (P0): gen-dev-status.sh + Stop hook

### Task 2.1: 创建 gen-dev-status.sh 脚本

**Files:**
- Create: `scripts/gen-dev-status.sh`

- [ ] **Step 1: 创建脚本**

Write: `scripts/gen-dev-status.sh`

```bash
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
    auto_gen_marker
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
```

- [ ] **Step 2: 设置执行权限并测试**

```bash
chmod +x scripts/gen-dev-status.sh
./scripts/gen-dev-status.sh
cat .claude/DEVELOPMENT_STATUS.md | head -30
```

Expected: 文件以 AUTO-GENERATED 标记开头，包含 This Week / Active Changes / Recent Decisions / Links 四个 section。

- [ ] **Step 3: Commit**

```bash
git add scripts/gen-dev-status.sh .claude/DEVELOPMENT_STATUS.md
git commit -m "feat: add gen-dev-status.sh — auto-generate session-level task board"
```

---

### Task 2.2: 创建 Stop hook 脚本

**Files:**
- Create: `.claude/hooks/stop-gen-status.sh`

- [ ] **Step 1: 创建 hook 脚本**

Write: `.claude/hooks/stop-gen-status.sh`

```bash
#!/bin/bash
# stop-gen-status.sh — Stop hook: regenerate DEVELOPMENT_STATUS.md on session end

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

echo "=== Stop hook: regenerating DEVELOPMENT_STATUS.md ==="
bash "$PROJECT_ROOT/scripts/gen-dev-status.sh"
echo "=== Stop hook complete ==="
```

- [ ] **Step 2: 设置执行权限并测试**

```bash
chmod +x .claude/hooks/stop-gen-status.sh
bash .claude/hooks/stop-gen-status.sh
```

Expected: 输出 "=== Stop hook complete ==="

- [ ] **Step 3: Commit**

```bash
git add .claude/hooks/stop-gen-status.sh
git commit -m "feat: add Stop hook — auto-regenerate DEVELOPMENT_STATUS.md"
```

---

### Task 2.3: 注册 Stop hook 到 settings.local.json

**Files:**
- Modify: `.claude/settings.local.json`

- [ ] **Step 1: 添加 Stop hook**

在 `hooks` 对象中添加：

```json
"Stop": [
  {
    "hooks": [
      {
        "type": "command",
        "command": "bash .claude/hooks/stop-gen-status.sh"
      }
    ]
  }
]
```

- [ ] **Step 2: 验证 JSON 格式**

```bash
python3 -c "import json; json.load(open('.claude/settings.local.json'))"
```

- [ ] **Step 3: Commit**

```bash
git add .claude/settings.local.json
git commit -m "chore: register Stop hook for DEVELOPMENT_STATUS.md generation"
```

---

## Phase 3 (P1): LLM-as-Judge 集成到 workflow-adopter

### Task 3.1: 添加 Judge 评分门禁到 workflow-adopter SKILL.md

**Files:**
- Modify: `.claude/skills/workflow-adopter/SKILL.md`

- [ ] **Step 1: 读取当前 SKILL.md**

Read: `.claude/skills/workflow-adopter/SKILL.md`

需要在 Step 2 各阶段的 Quality Gate 之后添加 LLM-as-Judge 评分逻辑。

- [ ] **Step 2: 在 "Step 3: Evaluate Gate" 之前插入 Judge 评分段落**

在 SKILL.md 第 382 行（Step 3 之前）添加：

```markdown
### Step 2.5: LLM-as-Judge Quality Gate (CEK Pattern)

每个阶段执行完毕后，调用 Judge 子代理评分。评分维度和阈值来自 CEK Reflexion 模式。

**评分维度与权重：**

| 维度 | 权重 | 说明 |
|------|------|------|
| 指令遵循 (Instruction Following) | 30% | 是否严格按 spec/plan/模板执行 |
| 输出完整性 (Output Completeness) | 25% | 所有需求是否覆盖 |
| 方案质量 (Solution Quality) | 25% | 代码/文档质量、测试覆盖 |
| 推理质量 (Reasoning Quality) | 10% | 决策是否有理据 |
| 响应连贯性 (Response Coherence) | 10% | 文档/代码风格一致 |

**阶段最低评分阈值：**

| 阶段 | 最低加权分 | 关键检查项 |
|------|-----------|-----------|
| Propose → Review | 3.5 | 范围清晰、需求明确、无 TBD |
| Review → Spec | 3.5 | 所有 Critical issue 已解决 |
| Spec → Design | 4.0 | 架构合理、边界清晰、API 完整 |
| Design → Plan | 3.5 | 任务 ≤ 4h、有验收标准 |
| Plan → Apply | 3.5 | 每个 task 有验收标准、并行组识别 |
| Apply → Deliver | 3.5 | 测试通过、代码质量达标 |
| Deliver → Archive | 4.0 | 无 CRITICAL/HIGH、Reflexion 评分完成 |

**Judge 执行流程：**

```
1. 读取当前阶段产出文件（proposal.md / spec.md / design.md / tasks.md / 代码）
2. 按 5 个维度逐项评分（1-5 分）
3. 计算加权总分
4. 如果总分 < 阶段阈值：
   a. 列出具体问题和改进建议
   b. 返回 gate_result: fail + 详细评分报告
5. 如果总分 ≥ 阶段阈值：
   a. 返回 gate_result: pass + 评分报告
6. 将评分报告写入 .claude/changes/<feature>/judge-report-<phase>.md
```

**Judge 子代理 Prompt 模板：**

```
Task(subagent_type="general-purpose", name="judge-<phase>",
  prompt="你是质量评审 Judge。严格评审以下产出。

评审维度（每项 1-5 分）：
1. 指令遵循 (30%): 是否严格按要求执行
2. 输出完整性 (25%): 所有需求是否覆盖
3. 方案质量 (25%): 质量、测试、可维护性
4. 推理质量 (10%): 决策是否有理据
5. 响应连贯性 (10%): 风格一致

默认分 2 分。必须有证据才能加分。5 分极其罕见 (<5%)。

输出格式：
## Judge Report — <phase>
| 维度 | 分数 | 证据 |
|------|------|------|
| 指令遵循 | X/5 | ... |
| 输出完整性 | X/5 | ... |
| 方案质量 | X/5 | ... |
| 推理质量 | X/5 | ... |
| 响应连贯性 | X/5 | ... |
加权总分: X.XX/5.0
Verdict: PASS/FAIL (阈值: X.X)

[产出内容]")
```

**评分报告持久化：**

每次 Judge 评分后，写入：
`.claude/changes/<feature>/judge-report-<phase>.md`

Archive 阶段将所有 Judge 报告合并为 Reflexion 评分总报告：
`docs/archive/<feature>-<date>/reflexion-report.md`
```

- [ ] **Step 3: 更新 Step 3: Evaluate Gate 逻辑**

在现有的 Step 3 中，将 gate 评估逻辑扩展为：先执行原有 checklist，再检查 Judge 评分是否 ≥ 阈值。

在 "Step 3: Evaluate Gate" 的第 386-398 行区域，在现有逻辑之后添加：

```markdown
**附加检查**：如果 Step 2.5 的 Judge 评分报告存在且 Verdict=FAIL，即使 checklist 全部通过，gate 也判定为 fail。Judge 评分是硬性门禁，不可跳过。
```

- [ ] **Step 4: 更新 Archive 阶段，添加 Reflexion 评分总报告**

在 Archive 阶段（第 364-383 行）的 Process 列表中，在现有步骤之后添加：

```markdown
7. **合并 Reflexion 评分报告**: 收集 `.claude/changes/<feature>/judge-report-*.md`，合并为 `docs/archive/<feature>-<date>/reflexion-report.md`，包含：
   - 每个阶段的 Judge 评分
   - 总体加权平均分
   - 主要改进项和已解决项

**Quality Gate 添加**：
- [ ] reflexion-report.md 已写入 archive
```

- [ ] **Step 5: 更新 Final Checklist**

在第 433-448 行的 Final Checklist 中添加：

```markdown
- [ ] All Judge reports collected in reflexion-report.md
- [ ] Overall weighted score ≥ 3.5
```

- [ ] **Step 6: Commit**

```bash
git add .claude/skills/workflow-adopter/SKILL.md
git commit -m "feat: integrate LLM-as-Judge quality gates into workflow-adopter (CEK pattern)"
```

---

## Phase 4 (P1): CI 一致性校验

### Task 4.1: 创建 lint-docs-consistency.sh

**Files:**
- Create: `scripts/lint-docs-consistency.sh`

- [ ] **Step 1: 创建校验脚本**

Write: `scripts/lint-docs-consistency.sh`

```bash
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
ERRORS=0
WARNINGS=0

echo "=== Docs Consistency Lint ==="
echo ""

# --- Check 1: YAML frontmatter format ---
echo "[CHECK 1] YAML frontmatter format..."
for f in docs/specs/*.md docs/plans/*.md docs/designs/*.md docs/decisions/*.md docs/standards/*.md; do
    [ -f "$f" ] || continue
    # Check for required fields
    for field in topic stage version status; do
        if ! grep -q "^${field}:" "$f" 2>/dev/null; then
            echo "  HIGH: $(basename "$f") missing required field: $field"
            ERRORS=$((ERRORS + 1))
        fi
    done
done

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
```

- [ ] **Step 2: 设置执行权限并测试**

```bash
chmod +x scripts/lint-docs-consistency.sh
./scripts/lint-docs-consistency.sh
echo "Exit code: $?"
```

Expected: 输出检查结果，exit code 0 (pass) 或 2 (warnings)。

- [ ] **Step 3: Commit**

```bash
git add scripts/lint-docs-consistency.sh
git commit -m "feat: add lint-docs-consistency.sh — CI linter for docs/ vs code consistency"
```

---

## Phase 5 (P2): Doc-Gardening Agent + 新增标准文档

### Task 5.1: 创建 doc-gardening agent prompt

**Files:**
- Create: `.claude/agents/doc-gardener.md`

- [ ] **Step 1: 创建 agent 定义**

Write: `.claude/agents/doc-gardener.md`

```markdown
---
name: doc-gardener
description: Periodic doc-gardening agent. Scans docs/ vs code actual state, generates scoring report, and creates fix PRs for drift. Use when "doc-gardening", "scan docs consistency", or "weekly doc cleanup".
tools: Bash, Read, Grep, Glob, Write
---

# Doc-Gardener

You are a doc-gardening agent inspired by the OpenAI Harness pattern and CEK Kaizen continuous improvement philosophy.

Your job: Scan docs/ vs code actual state, score consistency, and create fix PRs for drift.

## Process

### 1. Scan docs/ Status

Read all docs/ files with YAML frontmatter. Extract:
- topic, stage, version, status
- Last modified time

### 2. Code Reality Check

For each doc with status=已批准:

**Specs:**
- Parse API endpoint definitions
- Check each endpoint exists in controller code
- Flag: endpoint in spec but not in code (stale spec)
- Flag: endpoint in code but not in spec (undocumented)

**Designs:**
- Check C4 diagrams match actual module dependencies
- Flag: module in design but not in codebase
- Flag: module in codebase but not in design

**Plans:**
- Check task completion status
- Flag: task marked complete but code not found
- Flag: code exists but task not marked complete

### 3. Wiki Drift Check

Compare wiki/ auto-generated content vs actual state:
- Are all entities documented?
- Are all controllers documented?
- Is gaps.md up to date?

### 4. Scoring Report

Generate scoring report:

| Dimension | Score | Details |
|-----------|-------|---------|
| Spec Coverage | X/5 | API endpoint coverage |
| Design Accuracy | X/5 | Module dependency match |
| Plan Freshness | X/5 | Task completion accuracy |
| Wiki Completeness | X/5 | Entity/controller coverage |
| Overall | X.XX/5.0 | Weighted average |

### 5. Create Fix PRs

For HIGH severity drift items:
1. Create branch: `doc-gardening/YYYY-MM-DD`
2. Fix stale/missing wiki entries
3. Update gaps.md
4. Open PR with scoring report as description

For MEDIUM items:
- Add to gaps.md for human review
- Do not auto-fix

## Scoring Criteria (CEK Reflexion)

- **5**: Perfect alignment, no gaps
- **4**: Minor gaps, easily fixable
- **3**: Some drift, requires attention
- **2**: Significant drift, many gaps
- **1**: Severely outdated, needs full rewrite

## Output

Always produce:
1. Scoring report (printed to stdout)
2. Fix PR (if HIGH items found)
3. Updated gaps.md
```

- [ ] **Step 2: Commit**

```bash
git add .claude/agents/doc-gardener.md
git commit -m "feat: add doc-gardener agent — periodic docs/ vs code consistency scanner"
```

---

### Task 5.2: 创建 docs/ ↔ wiki/ 同步规范标准文档

**Files:**
- Create: `docs/standards/2026-05-02-v1.0-doc-sync-rules.md`

- [ ] **Step 1: 创建标准文档**

Write: `docs/standards/2026-05-02-v1.0-doc-sync-rules.md`

```markdown
---
topic: doc-sync-rules
stage: standard
version: v1.0
status: 已批准
supersedes: ""
---

# Docs / Wiki 同步规范

> **主题**: docs/ ↔ wiki/ ↔ DEVELOPMENT_STATUS.md 三体系强约束联动
> **阶段**: standard
> **版本**: v1.0
> **状态**: 已批准
> **日期**: 2026-05-02

---

## 1. 架构总览

三套文档体系的写入权限和约束：

| 体系 | 角色 | 写入权限 | 约束机制 |
|------|------|---------|---------|
| `docs/` | SSOT 权威基线 | 人工/AI 可写 | SDD 评审流程 |
| `wiki/` | 派生视图 | 只读（脚本生成） | PreCommit hook |
| `DEVELOPMENT_STATUS.md` | 会话看板 | 只读（hook 生成） | Stop hook |

## 2. 同步规则

### 2.1 触发时机

| 时机 | 动作 | 脚本 |
|------|------|------|
| git commit (docs/ 变更) | PreCommit hook 强制调用 sync-wiki.sh | `scripts/sync-wiki.sh` |
| 会话结束 | Stop hook 生成 DEVELOPMENT_STATUS.md | `scripts/gen-dev-status.sh` |
| CI 定期 (每周) | doc-gardener agent 全量扫描 | `.claude/agents/doc-gardener.md` |
| 手动触发 | 开发者手动调用 | `./scripts/sync-wiki.sh` |

### 2.2 同步内容

| wiki/ 文件 | 数据源 | 更新频率 |
|-----------|--------|---------|
| `log.md` | git log + docs/ YAML status | 每次 docs/ 变更 |
| `active-areas.md` | docs/specs/ + docs/plans/ status=进行中 | 每次 docs/ 变更 |
| `gaps.md` | 代码扫描 vs wiki/ 现有文档 | 每次 docs/ 变更 |
| `decisions.md` | docs/decisions/ ADR 索引 | 每次 docs/ 变更 |
| `technical-debt.md` | docs/ + code TODO/FIXME | 每周 (doc-gardening) |

## 3. Poka-Yoke 防错规则

### 3.1 自动标记

所有脚本生成的文件必须以标记开头：
```
<!-- AUTO-GENERATED: <script-name> at YYYY-MM-DDTHH:MM:SSZ -->
```

### 3.2 PreCommit 拦截

| 检测条件 | 拦截行为 |
|---------|---------|
| docs/ 变更但未运行 sync-wiki.sh | 阻止提交 |
| wiki/ 文件被手动编辑（无 AUTO-GENERATED 标记） | 阻止提交 |
| DEVELOPMENT_STATUS.md 被手动编辑 | 阻止提交 |

### 3.3 CI 校验

`scripts/lint-docs-consistency.sh` 检查：
- YAML frontmatter 格式完整
- wiki/ 文件有 AUTO-GENERATED 标记
- Entity/Controller 文档覆盖率
- Exit code: 0=pass, 1=HIGH fail, 2=MEDIUM warnings

## 4. Doc-Gardening

定期由 `.claude/agents/doc-gardener.md` 执行：

1. 扫描 docs/ vs 代码实际状态
2. 生成评分报告（5 维度，1-5 分）
3. HIGH 偏差 → 自动创建修复 PR
4. MEDIUM 偏差 → 记入 gaps.md

评分维度：Spec 覆盖率、设计准确性、计划新鲜度、Wiki 完整性、综合评分。

## 5. 引用

- wiki/ 管理规范: `docs/standards/2026-04-30-v1.0-wiki-constraints.md`
- SDD 流程: `docs/standards/sdd-process.md`
- Feature 工作流: `docs/standards/2026-04-30-v1.0-feature-workflow.md`
```

- [ ] **Step 2: Commit**

```bash
git add docs/standards/2026-05-02-v1.0-doc-sync-rules.md
git commit -m "docs: add doc-sync-rules standard — three-system enforcement spec"
```

---

### Task 5.3: 更新 GUIDE.md — 加入 LLM-as-Judge 说明

**Files:**
- Modify: `.claude/workflow/GUIDE.md`

- [ ] **Step 1: 读取 GUIDE.md 找到插入位置**

Read: `.claude/workflow/GUIDE.md`

在文档的适当位置（质量门禁相关段落之后）添加 LLM-as-Judge 说明。

- [ ] **Step 2: 添加 LLM-as-Judge 段落**

在 GUIDE.md 中添加一个新 section：

```markdown
## LLM-as-Judge 质量门禁（CEK 集成）

借鉴 CEK (Context Engineering Kit) 的 LLM-as-Judge 验证模式，每个阶段转换时注入自动评分。

### 评分维度

| 维度 | 权重 | 说明 |
|------|------|------|
| 指令遵循 | 30% | 是否严格按 spec/plan 执行 |
| 输出完整性 | 25% | 所有需求是否覆盖 |
| 方案质量 | 25% | 代码质量、测试覆盖 |
| 推理质量 | 10% | 决策是否有理据 |
| 响应连贯性 | 10% | 文档/代码风格一致 |

### 阶段阈值

| 阶段转换 | 最低分 |
|---------|--------|
| Propose → Review | 3.5 |
| Spec → Design | 4.0 |
| Design → Plan | 3.5 |
| Plan → Apply | 3.5 |
| Deliver → Archive | 4.0 |

### 使用方式

workflow-adopter 自动集成 Judge 评分。详见 `.claude/skills/workflow-adopter/SKILL.md` Step 2.5。
```

- [ ] **Step 3: Commit**

```bash
git add .claude/workflow/GUIDE.md
git commit -m "docs: add LLM-as-Judge section to workflow GUIDE"
```

---

### Task 5.4: 更新 feature-workflow.md — Archive 阶段加入 Reflexion

**Files:**
- Modify: `docs/standards/2026-04-30-v1.0-feature-workflow.md`

- [ ] **Step 1: 读取 feature-workflow.md 找到 Archive 阶段**

Read: `docs/standards/2026-04-30-v1.0-feature-workflow.md`

- [ ] **Step 2: 在 Archive 阶段添加 Reflexion 评分要求**

在 Archive 阶段的描述中添加：

```markdown
**Archive 阶段附加要求**:
- 收集本次变更所有 Judge 评分报告
- 合并为 `docs/archive/<feature>-<date>/reflexion-report.md`
- 包含：每个阶段评分、加权平均分、主要改进项
- 总体加权分 ≥ 3.5 方可归档
```

- [ ] **Step 3: Commit**

```bash
git add docs/standards/2026-04-30-v1.0-feature-workflow.md
git commit -m "docs: add Reflexion scoring to Archive phase in feature-workflow"
```

---

## Self-Review

### Spec 覆盖率

| Spec 需求 | 对应 Task |
|----------|----------|
| sync-wiki.sh 脚本 | Task 1.1 |
| PreCommit hook | Task 1.2, 1.3 |
| gen-dev-status.sh 脚本 | Task 2.1 |
| Stop hook | Task 2.2, 2.3 |
| LLM-as-Judge 集成 | Task 3.1 |
| lint-docs-consistency.sh | Task 4.1 |
| doc-gardening agent | Task 5.1 |
| 同步规范标准文档 | Task 5.2 |
| GUIDE.md 更新 | Task 5.3 |
| feature-workflow.md 更新 | Task 5.4 |
| wiki-constraints.md 更新 | Task 0.2 |

所有 spec 需求已覆盖。

### 占位符扫描

- 无 TBD/TODO 占位符
- 所有步骤包含完整代码
- 所有文件路径精确

### 类型一致性

- 所有脚本使用 `scripts/lib/common.sh` 共享函数
- 所有 hook 脚本使用 `$PROJECT_ROOT` 统一路径
- YAML frontmatter 字段名统一：topic, stage, version, status
