# Language Analyzer Decoupling Plan

## Goal

Split the Java and Kotlin analyzer implementations out of `core` while keeping all reusable CDD logic in `core`.

This plan does **not** cover IntelliJ plugin implementation. The scope is limited to modularizing the current project so language-specific parser dependencies no longer live in `core`.

## Target Module Structure

```text
core/
languages/
  java/
  kotlin/
cli/
```

Gradle modules:

- `:core`
- `:languages:java`
- `:languages:kotlin`
- `:cli`

## Architectural Boundary

### `:core`

`core` remains the reusable engine and must contain only logic that is independent of the Java and Kotlin parser stacks.

Keep in `core`:

- domain models
- analyzer contracts
- configuration loading and parsing
- package detection
- file scanning
- ICP aggregation
- report generation
- shared utilities that are parser-agnostic

Remove from `core`:

- Spoon dependency
- Kotlin compiler dependency
- concrete Java analyzer implementation
- concrete Kotlin analyzer implementation

### `:languages:java`

This module owns all Java-specific analysis logic and dependencies.

Keep here:

- Spoon dependency
- `JavaAnalyzer`
- `JavaCtScanner`
- Java analyzer tests and fixtures if they are only relevant to Java parsing

### `:languages:kotlin`

This module owns all Kotlin-specific analysis logic and dependencies.

Keep here:

- Kotlin compiler / PSI dependency used by the current implementation
- `KotlinAnalyzer`
- `KotlinIcpScanner`
- Kotlin analyzer tests and fixtures if they are only relevant to Kotlin parsing

### `:cli`

The CLI becomes the composition root for analyzer registration.

Responsibilities:

- depend on `:core`
- depend on `:languages:java`
- depend on `:languages:kotlin`
- register concrete analyzers
- keep CLI-only behavior and output handling

## Current Coupling To Remove

The current `core` module is coupled to language runtimes through:

- `fr.inria.gforge.spoon:spoon-core` in `core/build.gradle.kts`
- `org.jetbrains.kotlin:kotlin-compiler` in `core/build.gradle.kts`
- Java analyzer classes under `core/src/main/kotlin/com/cdd/analyzer/java`
- Kotlin analyzer classes under `core/src/main/kotlin/com/cdd/analyzer/kotlin`

This makes `core` non-reusable as a clean library because parser/runtime choices are embedded in the shared artifact.

## Public Contract To Keep Stable

The reusable API should stay in `core`.

Current contract:

- `com.cdd.analyzer.LanguageAnalyzer`
- `com.cdd.analyzer.AbstractLanguageAnalyzer`
- `com.cdd.domain.*`
- shared config and reporter contracts

The first pass should keep this contract stable to minimize migration risk. The goal is relocation and dependency cleanup before redesign.

## Concrete File Moves

### Move to `:languages:java`

Move these files out of `core`:

- `core/src/main/kotlin/com/cdd/analyzer/java/JavaAnalyzer.kt`
- `core/src/main/kotlin/com/cdd/analyzer/java/JavaCtScanner.kt`

Move these tests:

- `core/src/test/kotlin/com/cdd/analyzer/java/JavaAnalyzerTest.kt`
- Java-specific sample fixtures under `core/src/test/resources/java-samples`
- `core/src/test/resources/sample-java/SimpleJava.java`

### Move to `:languages:kotlin`

Move these files out of `core`:

- `core/src/main/kotlin/com/cdd/analyzer/kotlin/KotlinAnalyzer.kt`
- `core/src/main/kotlin/com/cdd/analyzer/kotlin/KotlinIcpScanner.kt`

Move these tests:

- `core/src/test/kotlin/com/cdd/analyzer/kotlin/KotlinAnalyzerTest.kt`
- Kotlin-specific sample fixtures under `core/src/test/resources/kotlin-samples`
- `core/src/test/resources/sample-kotlin/SimpleKotlin.kt`

### Keep in `:core`

Keep these packages in `core`:

- `com.cdd.analyzer` for the shared analyzer contract
- `com.cdd.core.aggregator`
- `com.cdd.core.config`
- `com.cdd.core.registry`
- `com.cdd.core.scanner`
- `com.cdd.core.util`
- `com.cdd.domain`
- `com.cdd.reporter`

Review mixed fixtures under `core/src/test/resources/sample-mixed` and keep them in the module where they provide the most value. If they validate cross-module behavior through `cli`, move them to CLI integration tests.

## Gradle Refactor Plan

### 1. Update `settings.gradle.kts`

Add:

- `include(":languages:java")`
- `include(":languages:kotlin")`

### 2. Shrink `:core`

Remove from `core/build.gradle.kts`:

- Spoon
- Kotlin compiler

Keep only reusable dependencies such as:

- serialization
- YAML
- markdown
- logging API if still needed

If logging stays, prefer `slf4j-api` in reusable modules rather than a concrete binding in `core`.

### 3. Create `languages/java/build.gradle.kts`

Dependencies:

- `implementation(project(":core"))`
- `implementation("fr.inria.gforge.spoon:spoon-core:11.2.1")`
- Java analyzer test dependencies

### 4. Create `languages/kotlin/build.gradle.kts`

Dependencies:

- `implementation(project(":core"))`
- `implementation("org.jetbrains.kotlin:kotlin-compiler:2.3.0")`
- Kotlin analyzer test dependencies

### 5. Update `cli/build.gradle.kts`

Replace the single `:core` dependency with:

- `implementation(project(":core"))`
- `implementation(project(":languages:java"))`
- `implementation(project(":languages:kotlin"))`

## Registration Strategy

Keep analyzer registration in the application layer.

The current CLI registration flow is correct in principle:

- instantiate concrete analyzers
- register them in `AnalyzerRegistry`

After the split, the CLI should import analyzers from:

- `:languages:java`
- `:languages:kotlin`

`core` should continue to expose only the registry and analyzer contract, not concrete language implementations.

## Shared Logic Cleanup

After the mechanical split, review duplicated logic and move only reusable pieces back into `core`.

Likely candidates:

- SLOC calculation helpers
- common line and column helpers
- shared analyzer error handling patterns

Do not force a generic abstraction too early. If duplication is small and parser-specific, keep it in the language module.

## Migration Phases

### Phase 1: Mechanical Module Extraction

Objective:

- move files with minimal behavior change

Tasks:

- create `languages/java`
- create `languages/kotlin`
- add Gradle module includes
- move Java analyzer source and tests
- move Kotlin analyzer source and tests
- move parser dependencies to the correct module
- update imports and package references
- update CLI dependencies
- update CLI analyzer registration imports

Acceptance criteria:

- project builds successfully
- existing CLI behavior is unchanged
- Java analyzer tests pass from `:languages:java`
- Kotlin analyzer tests pass from `:languages:kotlin`

### Phase 2: Core Cleanup

Objective:

- leave `core` as a reusable engine with no parser coupling

Tasks:

- remove leftover parser-specific utilities from `core`
- move any neutral helpers discovered during extraction into `core`
- reduce runtime-specific dependencies in `core`
- ensure `core` can be published or reused without Java/Kotlin parser baggage

Acceptance criteria:

- `:core` has no Spoon dependency
- `:core` has no Kotlin compiler dependency
- `:core` contains no imports from `spoon.*`
- `:core` contains no imports from `org.jetbrains.kotlin.cli.*` or `org.jetbrains.kotlin.psi.*`

### Phase 3: Verification And Hardening

Objective:

- verify the split did not change analysis results

Tasks:

- compare CLI output before and after the refactor on the existing sample fixtures
- run module-specific tests
- add one CLI integration test that exercises both analyzers through registration
- verify file discovery still works through `AnalyzerRegistry`

Acceptance criteria:

- report output is unchanged for representative Java and Kotlin samples
- analyzer registration still resolves by file extension
- no transitive parser dependency leaks back into `:core`

## Risks

### Risk 1: Hidden dependencies inside `core`

Some utilities in `core` may indirectly assume Java/Kotlin analyzer presence through imports or tests.

Mitigation:

- search for all references to Java/Kotlin analyzer packages before moving files
- move tests to the correct module with the implementation they validate

### Risk 2: Fixture placement becomes inconsistent

Shared sample resources may become hard to locate after the split.

Mitigation:

- place language-specific fixtures with the language module
- keep only truly reusable test fixtures in `core`
- keep cross-language end-to-end fixtures in `cli`

### Risk 3: Logging and runtime bindings leak into reusable modules

`core` currently carries a concrete logging implementation.

Mitigation:

- prefer `slf4j-api` in reusable modules
- leave concrete bindings to the executable application layer if possible

## Definition Of Done

The refactor is complete when all of the following are true:

- `core` contains only reusable CDD logic
- `languages/java` owns Java parsing and Java analyzer tests
- `languages/kotlin` owns Kotlin parsing and Kotlin analyzer tests
- `cli` assembles analyzers from the language modules
- the project structure matches:

```text
core/
languages/
  java/
  kotlin/
cli/
```

- `core` no longer depends on Spoon
- `core` no longer depends on the Kotlin compiler
- the CLI produces the same analysis output as before

## Recommended Execution Order

1. Add `languages/java` and `languages/kotlin` modules.
2. Move Java analyzer source and tests.
3. Move Kotlin analyzer source and tests.
4. Remove parser dependencies from `core`.
5. Update CLI dependencies and imports.
6. Run tests and compare output fixtures.
7. Clean up shared helpers only after the split is stable.
