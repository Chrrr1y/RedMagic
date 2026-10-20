# RedMagic Helper Build Rules

## Versioning

- `version.txt` stores the version name from the latest successful default Release build.
- `./scripts/build-release.sh` increments the patch component before compiling and records the new version only after the build artifacts are generated.
- An explicit `VERSION_NAME` overrides automatic version incrementing and does not update `version.txt`.
- Never append `-rewrite` to `versionName`.
- Every local build gets an automatic numeric `versionCode` from the current Unix timestamp unless `VERSION_CODE` is explicitly provided.
- CI or release scripts may provide `VERSION_CODE`, `VERSION_NAME`, and `RELEASE_TAG` through the environment.
- Release tags use `v<versionName>-build-<versionCode>`.

## Git Commit Convention

- Use Conventional Commits: `<type>(<scope>): <imperative summary>`. Omit the scope when it does not add useful context.
- Keep commit subjects in English to match the existing history, under 72 characters, and without a trailing period.
- Examples: `fix(update): fall back to the GitHub releases feed` and `docs(agent): define commit conventions`.
- Use `feat` for user-facing features, `fix` for bug fixes, `perf` for performance work, `refactor` for behavior-preserving code changes, `ui` for visual changes, `build` for build tooling, `docs` for documentation, and `chore` for maintenance.
- Keep each commit focused on one logical change. Explain non-obvious decisions in the commit body; mark incompatible changes with `!` and document them under `BREAKING CHANGE:`.
- Before committing, review `git status --short`, `git diff --check`, and the staged diff. Stage explicit paths instead of using `git add .`.
- Commit source, documentation, and required project configuration only. Never commit APKs, build outputs, release bundles, Perfetto traces, signing keys, credentials, or machine-local configuration.
- Do not rewrite published commit history unless the task explicitly requires it.

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
- Releases are published locally through the authenticated `gh` CLI. Do not add GitHub Actions workflows for builds or releases.
