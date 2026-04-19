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
