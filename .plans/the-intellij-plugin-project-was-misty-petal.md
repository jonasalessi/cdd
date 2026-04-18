# Split GitHub CI per module (icp-cli and intellij-plugin)

## Context

The `intellij-plugin/` project was recently imported from a standalone repo into the
`cdd` monorepo as an included Gradle build. It brought its own workflows under
`intellij-plugin/.github/workflows/`, but those are **inert**: GitHub Actions only
executes workflows at the repo root `.github/workflows/`. At the same time, the root
workflows (`.github/workflows/build.yml`, `.github/workflows/release.yml`) were written
for the CLI only but trigger on every change anywhere and ignore the plugin entirely.

Goal: produce two independent CI pipelines — one per module — that

- trigger only when their module changes (path filters),
- run `./gradlew` inside the correct module directory, and
- reuse the plugin's original, more sophisticated workflows (build/test/Qodana/verify/draft‑release,
  publish‑release, UI tests) instead of rewriting them.

## Current state (relevant files)

- `/.github/workflows/build.yml` — generic `clean build`, no path filter, runs for both modules.
- `/.github/workflows/release.yml` — references `:cli:shadowJar :cli:distZip`; intended for icp-cli.
- `/intellij-plugin/.github/workflows/build.yml` — 4-job pipeline (build, test, inspectCode/Qodana, verify, releaseDraft). Inert.
- `/intellij-plugin/.github/workflows/release.yml` — signs and publishes to JetBrains Marketplace. Inert.
- `/intellij-plugin/.github/workflows/run-ui-tests.yml` — manual dispatch, matrix across OSes. Inert.
- `/intellij-plugin/.github/dependabot.yml` — Gradle + GH Actions daily updates. Inert (Dependabot only reads `/.github/dependabot.yml` at root).
- `/intellij-plugin/codecov.yml`, `/intellij-plugin/qodana.yml` — tool configs consumed by the plugin CI.
- `/settings.gradle.kts` uses `includeBuild("icp-cli")` and `includeBuild("intellij-plugin")`. Each module has its own `gradlew` and its own build.

Every existing plugin workflow assumes `./gradlew …` runs from the plugin's own root and paths like `${{ github.workspace }}/build/...` live at repo root. Once moved up to `/.github/workflows/`, these need `working-directory: intellij-plugin` (or `-p intellij-plugin`) and path rewrites to `intellij-plugin/build/...`.

## Target layout

```
/.github/
├── dependabot.yml                         # consolidated
└── workflows/
    ├── icp-cli-build.yml                  # replaces current root build.yml (CLI-scoped)
    ├── icp-cli-release.yml                # replaces current root release.yml
    ├── intellij-plugin-build.yml          # ported from intellij-plugin/.github/workflows/build.yml
    ├── intellij-plugin-release.yml        # ported from intellij-plugin/.github/workflows/release.yml
    └── intellij-plugin-ui-tests.yml       # ported from intellij-plugin/.github/workflows/run-ui-tests.yml
```

After the move, delete `intellij-plugin/.github/` entirely (workflows + dependabot.yml) so the repo has a single source of truth.

Keep `intellij-plugin/codecov.yml` and `intellij-plugin/qodana.yml` inside the plugin dir — they are plugin-specific and the workflows point at them explicitly.

## Changes per workflow

### 1. `icp-cli-build.yml` (new, replaces root `build.yml`)

- **Triggers:**
  - `push` to `main` with `paths: [ 'icp-cli/**', '.github/workflows/icp-cli-build.yml' ]`
  - `pull_request` with same `paths`
- **Job:** build + test
  - JDK 21 Temurin, `gradle/actions/setup-gradle@v5`
  - `./gradlew clean build` with `working-directory: icp-cli`
  - Upload test reports from `icp-cli/build/reports/tests` on failure
  - Upload coverage XML from `icp-cli/build/reports/jacoco/coverageReport/coverageReport.xml` to Codecov (optional; ask user)
- No plugin-style artifact upload (no buildPlugin output).

### 2. `icp-cli-release.yml` (new, replaces root `release.yml`)

- **Trigger:** `release: [created]` filtered by tag prefix (e.g., `cli-v*`) — enforced by `if:` on the job since `release` events can't be filtered at the workflow level.
- Checkout at the release tag.
- JDK 21 Temurin, setup Gradle.
- `./gradlew :cli:shadowJar :cli:distZip` with `working-directory: icp-cli`.
- Upload assets via `softprops/action-gh-release@v2`:
  - `icp-cli/cli/build/distributions/*.zip`
  - `icp-cli/cli/build/libs/cdd-cli.jar`

### 3. `intellij-plugin-build.yml` (ported from plugin repo)

Keep the 4-job structure + releaseDraft. Adjust:

- **Triggers:**
  - `push: main`, `pull_request`, with `paths: [ 'intellij-plugin/**', 'icp-cli/core/**', '.github/workflows/intellij-plugin-build.yml' ]`
    (Plugin depends on `icp-cli:core` via `includeBuild`, so core changes must revalidate the plugin.)
  - Keep `concurrency` block as is.
- **All `run: ./gradlew ...` steps:** add `working-directory: intellij-plugin`.
- **Artifact prep** (`cd ${{ github.workspace }}/build/distributions`): change to `cd ${{ github.workspace }}/intellij-plugin/build/distributions`.
- **Upload artifact `path:`**: `./intellij-plugin/build/distributions/content/*/*`.
- **Tests upload path:** `intellij-plugin/build/reports/tests`.
- **Codecov `files:`**: `intellij-plugin/build/reports/kover/report.xml`.
- **Qodana** (`JetBrains/qodana-action@v2025.1.1`): add `args: --project-dir,intellij-plugin` (Qodana CLI flag) so it scans the plugin subdir and picks up `intellij-plugin/qodana.yml`.
- **verifyPlugin artifact path:** `intellij-plugin/build/reports/pluginVerifier`.
- **releaseDraft:** `./gradlew properties ...` and `./gradlew getChangelog ...` both need `working-directory: intellij-plugin`; `RELEASE_NOTE="./build/tmp/release_note.txt"` becomes relative to that working dir, which is fine.
  - Draft tag: keep `gh release create $VERSION` but prefix — we'll align with the release workflow's tag filter (see question 1 below).

### 4. `intellij-plugin-release.yml` (ported from plugin repo)

- **Trigger:** `release: [prereleased, released]`, filtered by tag prefix `plugin-v*` via `if:` guard on the job.
- Checkout `ref: ${{ github.event.release.tag_name }}` (unchanged).
- All Gradle steps: `working-directory: intellij-plugin`.
- `gh release upload ${{ github.event.release.tag_name }} ./intellij-plugin/build/distributions/*` (adjust path).
- Secrets unchanged: `PUBLISH_TOKEN`, `CERTIFICATE_CHAIN`, `PRIVATE_KEY`, `PRIVATE_KEY_PASSWORD`, `GITHUB_TOKEN`.
- Changelog PR block stays (commits `intellij-plugin/CHANGELOG.md`).

### 5. `intellij-plugin-ui-tests.yml` (ported from plugin repo)

- Trigger: `workflow_dispatch` (unchanged).
- All Gradle invocations (including matrix commands) need to run inside `intellij-plugin/`:
  - For `run:` steps use `working-directory: intellij-plugin`.
  - Matrix `runIde` commands: replace `./gradlew ...` with `./gradlew -p intellij-plugin ...` (the matrix commands are expanded as a single string; `working-directory` applies to the step so either approach works — prefer `working-directory`).

### 6. `/.github/dependabot.yml` (new, merged from plugin's)

```yaml
version: 2
updates:
  - package-ecosystem: gradle
    directory: /icp-cli
    schedule: { interval: daily }
  - package-ecosystem: gradle
    directory: /intellij-plugin
    schedule: { interval: daily }
  - package-ecosystem: github-actions
    directory: /
    schedule: { interval: daily }
```

### 7. Deletions

- `/intellij-plugin/.github/workflows/build.yml`
- `/intellij-plugin/.github/workflows/release.yml`
- `/intellij-plugin/.github/workflows/run-ui-tests.yml`
- `/intellij-plugin/.github/dependabot.yml`
- `/.github/workflows/build.yml` (superseded by `icp-cli-build.yml`)
- `/.github/workflows/release.yml` (superseded by `icp-cli-release.yml`)

## Verification

1. `gh workflow list` after pushing the branch to confirm the 5 new workflows are registered and the old root ones are gone.
2. Make a no-op commit touching only `icp-cli/README.md` → only **icp-cli Build** runs.
3. Make a no-op commit touching only `intellij-plugin/README.md` → only **intellij-plugin Build** runs.
4. Make a no-op commit touching `icp-cli/core/` → **both** builds run (plugin depends on core).
5. Cut a draft release tagged `cli-v0.0.1-test` → only `icp-cli-release` runs; assets `cdd-cli.jar` + zip attached.
6. Cut a draft release tagged `plugin-v0.0.2-test` (draft, don't publish) → `intellij-plugin-release` runs, fails cleanly if JetBrains secrets aren't set (expected in a test run).
7. `gh workflow run "Run UI Tests"` → manual dispatch job launches on all three OSes.

## Critical files to touch

Create:
- `/.github/workflows/icp-cli-build.yml`
- `/.github/workflows/icp-cli-release.yml`
- `/.github/workflows/intellij-plugin-build.yml`
- `/.github/workflows/intellij-plugin-release.yml`
- `/.github/workflows/intellij-plugin-ui-tests.yml`
- `/.github/dependabot.yml`

Delete:
- `/.github/workflows/build.yml`
- `/.github/workflows/release.yml`
- `/intellij-plugin/.github/` (entire directory)

Unchanged:
- `/intellij-plugin/codecov.yml`
- `/intellij-plugin/qodana.yml`

## Decisions

1. **Release tags:** `cli-v*` for icp-cli, `plugin-v*` for the plugin. Each release workflow gates its job with `if: startsWith(github.event.release.tag_name, 'cli-v')` / `'plugin-v'`.
2. **icp-cli coverage:** enabled. The build runs `./gradlew clean build coverageReport` and uploads `icp-cli/build/reports/jacoco/coverageReport/coverageReport.xml` via `codecov/codecov-action@v5` using the `CODECOV_TOKEN` secret.
3. **Cleanup:** the entire `intellij-plugin/.github/` directory is deleted after the move. Git history preserves the originals.
