#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

version_name="${VERSION_NAME:-4.0.3}"
version_name="${version_name%-rewrite}"
version_code="${VERSION_CODE:-$(date -u +%s)}"
release_tag="${RELEASE_TAG:-v${version_name}-build-${version_code}}"
commit="$(git rev-parse --short HEAD 2>/dev/null || printf 'unknown')"
timestamp="$(date -u '+%Y-%m-%dT%H:%M:%SZ')"

write_values() {
    local file="$1"
    {
        printf 'VERSION_CODE=%s\n' "$version_code"
        printf 'VERSION_NAME=%s\n' "$version_name"
        printf 'RELEASE_TAG=%s\n' "$release_tag"
        printf 'BUILD_COMMIT=%s\n' "$commit"
        printf 'BUILD_TIMESTAMP=%s\n' "$timestamp"
    } >> "$file"
}

case "${1:---shell}" in
    --shell)
        printf 'export VERSION_CODE=%q\n' "$version_code"
        printf 'export VERSION_NAME=%q\n' "$version_name"
        printf 'export RELEASE_TAG=%q\n' "$release_tag"
        printf 'export BUILD_COMMIT=%q\n' "$commit"
        printf 'export BUILD_TIMESTAMP=%q\n' "$timestamp"
        ;;
    --env-file)
        if [[ $# -ne 2 ]]; then
            printf 'Usage: %s --env-file FILE\n' "$0" >&2
            exit 2
        fi
        write_values "$2"
        ;;
    --print)
        printf 'VERSION_CODE=%s\n' "$version_code"
        printf 'VERSION_NAME=%s\n' "$version_name"
        printf 'RELEASE_TAG=%s\n' "$release_tag"
        printf 'BUILD_COMMIT=%s\n' "$commit"
        printf 'BUILD_TIMESTAMP=%s\n' "$timestamp"
        ;;
    *)
        printf 'Usage: %s [--shell|--print|--env-file FILE]\n' "$0" >&2
        exit 2
        ;;
esac
