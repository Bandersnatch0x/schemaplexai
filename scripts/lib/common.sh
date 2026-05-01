#!/bin/bash
# common.sh — Shared functions for workflow scripts

set -euo pipefail

# Get project root (works from any subdirectory)
get_project_root() {
    git rev-parse --show-toplevel
}

# Generate AUTO-GENERATED marker
# Usage: auto_gen_marker [script-name]   (default: sync-wiki.sh)
auto_gen_marker() {
    local script_name="${1:-sync-wiki.sh}"
    echo "<!-- AUTO-GENERATED: ${script_name} at $(date -u +%Y-%m-%dT%H:%M:%SZ) -->"
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
    sed -n '/^---$/,/^---$/p' "$file" | grep "^${field}:" | head -1 | cut -d: -f2- | sed 's/^ *//;s/ *$//' || true
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
    if [ ! -f "$file" ]; then
        return 1
    fi
    local first_line=""
    read -r first_line < "$file"
    [[ "$first_line" == *AUTO-GENERATED:* ]]
}
