#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

version_name="${1:?version name is required}"
version_code="${2:?version code is required}"
release_tag="${3:?release tag is required}"
commit="$(git rev-parse --short HEAD 2>/dev/null || printf 'unknown')"
timestamp="$(date -u '+%Y-%m-%dT%H:%M:%SZ')"
previous_tag="$(git tag --list 'v*' --sort=-creatordate | head -n 1)"

if [[ -n "$previous_tag" ]]; then
    range="${previous_tag}..HEAD"
    range_label="since ${previous_tag}"
    updates="$(git log --no-merges --pretty=format:'- %s (%h)' "$range" 2>/dev/null || true)"
else
    range_label="recent commits"
    updates="$(git log --no-merges -20 --pretty=format:'- %s (%h)' HEAD 2>/dev/null || true)"
fi

if [[ -z "$updates" ]]; then
    updates="- No commit summary available"
fi

cat <<EOF
# RedMagic Helper ${version_name}

- Version code: \`${version_code}\`
- Release tag: \`${release_tag}\`
- Commit: \`${commit}\`
- Built: \`${timestamp}\`

## Updates (${range_label})

${updates}
EOF
