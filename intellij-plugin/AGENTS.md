# Plugin Architecture

## UI Components

All UI-related code is organized under `src/main/kotlin/com/cdd/ui/`.

- **Editor Inlays:** `src/main/kotlin/com/cdd/ui/editor/inlay/`
- **Main Settings:** `src/main/kotlin/com/cdd/ui/settings/tools/cdd/` (Matches `Settings > Tools > CDD`)
- **Actions on Save:** `src/main/kotlin/com/cdd/ui/settings/tools/actionsOnSave/` (Matches
  `Settings > Tools > Actions on Save`)

## Core Logic

- **Analyzers:** `src/main/kotlin/com/cdd/analyzer/` (Language-specific analysis logic)
- **Actions:** `src/main/kotlin/com/cdd/action/` (IntelliJ Actions/Menu items)
- **Listeners:** `src/main/kotlin/com/cdd/listener/` (Project/Application level event listeners)

## Settings Scope: Project vs. Personal

`cdd.yaml` stores the project's CDD definition (ICP limits, weights, coupling, filtering) — shared across the team,
checked into VCS, owned by `CddConfigService`.

Personal/IDE preferences (inlay font size, inlay position, and similar appearance/behavior toggles) must **NOT** be
written to `cdd.yaml`. They belong in an app-level `PersistentStateComponent` stored in the user's IDE config
(e.g. `cdd-inlay.xml`, `cdd-idea.xml`).

**Why:** `cdd.yaml` is a project definition — it travels with the repo and affects every contributor. Inlay appearance
is individual taste and would cause needless VCS churn / merge conflicts if stored there.

**How to apply:** When adding a new setting, first ask: is this a shared project rule, or a per-user preference?

- Shared rule → extend `CddSettingsModel` / `cdd.yaml` via `CddConfigService`.
- Per-user preference → new or existing `Service.Level.APP` `PersistentStateComponent` with its own `Storage(...)` file.

Never mix the two in one configurable.
