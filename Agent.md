# RedMagic Helper Build Rules

## Versioning

- The base application version is `4.0.4`.
- Never append `-rewrite` to `versionName`.
- Every local build gets an automatic numeric `versionCode` from the current Unix timestamp unless `VERSION_CODE` is explicitly provided.
- CI or release scripts may provide `VERSION_CODE`, `VERSION_NAME`, and `RELEASE_TAG` through the environment.
- Release tags use `v<versionName>-build-<versionCode>`.

## Build And Release

- Use `./scripts/build-release.sh` for a local Release build.
- Use `./scripts/build-release.sh --publish` to build, generate notes/checksums, and publish a GitHub Release through the authenticated `gh` CLI.
- Every build must generate version metadata and update notes under `app/build/generated/version/` and `dist/release/`.
- Do not run `gradle test` by default. Run tests only when the user explicitly requests them.
- A Release build without release keystore environment variables uses the debug signing key and may not upgrade an APK signed with another key.

## Update Notes

- Release notes are generated from Git commits since the newest `v*` tag.
- Keep commit messages concise and describe user-visible or engineering changes.
- Do not manually commit generated APKs, checksums, or release notes; they are ignored by Git.

## GitHub Authentication

- Before publishing locally, authenticate once with `gh auth login`.
- The publishing command requires repository contents write permission.
- GitHub Actions is a manual fallback only and must not be required for normal releases.
