#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

version_file="$ROOT_DIR/version.txt"

increment_patch_version() {
    local value="$1"
    if [[ ! "$value" =~ ^([0-9]+)\.([0-9]+)\.([0-9]+)$ ]]; then
        printf 'Cannot increment version "%s"; expected MAJOR.MINOR.PATCH.\n' "$value" >&2
        return 1
    fi
    local major="${BASH_REMATCH[1]}"
    local minor="${BASH_REMATCH[2]}"
    local patch=$((10#${BASH_REMATCH[3]} + 1))
    printf '%s.%s.%s\n' "$major" "$minor" "$patch"
}

record_version() {
    if [[ $# -ne 1 ]]; then
        printf 'Usage: %s --record VERSION_NAME\n' "$0" >&2
        exit 2
    fi
    local value="${1%-rewrite}"
    if [[ ! "$value" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
        printf 'Cannot record version "%s"; expected MAJOR.MINOR.PATCH.\n' "$value" >&2
        exit 2
    fi
    local temporary_file="${version_file}.tmp.$$"
    printf '%s\n' "$value" > "$temporary_file"
    mv "$temporary_file" "$version_file"
}

if [[ "${1:-}" == "--record" ]]; then
    shift
    record_version "$@"
    exit 0
fi

if [[ -n "${VERSION_NAME:-}" ]]; then
    version_name="${VERSION_NAME%-rewrite}"
else
    if [[ ! -f "$version_file" ]]; then
        printf 'Version file not found: %s\n' "$version_file" >&2
        exit 1
    fi
    last_version="$(tr -d '[:space:]' < "$version_file")"
    version_name="$(increment_patch_version "$last_version")"
fi
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
        printf 'Usage: %s [--shell|--print|--env-file FILE|--record VERSION_NAME]\n' "$0" >&2
        exit 2
        ;;
esac
