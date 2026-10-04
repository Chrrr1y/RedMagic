#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

publish=false
if [[ "${1:-}" == "--publish" ]]; then
    publish=true
elif [[ $# -gt 0 ]]; then
    printf 'Usage: %s [--publish]\n' "$0" >&2
    exit 2
fi

eval "$(scripts/version.sh --shell)"
export VERSION_CODE VERSION_NAME RELEASE_TAG BUILD_COMMIT BUILD_TIMESTAMP

release_dir="$ROOT_DIR/dist/release"
mkdir -p "$release_dir"
notes_file="$release_dir/RELEASE_NOTES.md"
apk_file="$release_dir/redmagic-helper-${VERSION_NAME}-${VERSION_CODE}.apk"
sha_file="${apk_file}.sha256"

scripts/release-notes.sh "$VERSION_NAME" "$VERSION_CODE" "$RELEASE_TAG" > "$notes_file"
./gradlew --no-daemon :app:assembleRelease

cp app/build/outputs/apk/release/app-release.apk "$apk_file"
sha256sum "$apk_file" > "$sha_file"
cp "$notes_file" "$release_dir/BUILD_NOTES.md"

printf 'Release APK: %s\n' "$apk_file"
printf 'Checksum: %s\n' "$sha_file"
printf 'Notes: %s\n' "$notes_file"
printf 'Version: %s (%s)\n' "$VERSION_NAME" "$VERSION_CODE"

if [[ "$publish" != true ]]; then
    printf 'GitHub upload skipped. Run %s --publish to create the release.\n' "$0"
    exit 0
fi

if ! command -v gh >/dev/null 2>&1; then
    printf 'GitHub CLI (gh) is required for --publish.\n' >&2
    exit 1
fi
gh auth status
repo="$(git config --get remote.origin.url)"
repo="${repo#git@github.com:}"
repo="${repo#https://github.com/}"
repo="${repo%.git}"
gh release create "$RELEASE_TAG" \
    "$apk_file" \
    "$sha_file" \
    --repo "$repo" \
    --target "$(git rev-parse HEAD)" \
    --title "RedMagic Helper ${VERSION_NAME} (${VERSION_CODE})" \
    --notes-file "$notes_file"
