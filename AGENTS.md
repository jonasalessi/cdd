# CDD CLI: Cognitive-Driven Development Analyzer

CDD CLI is a tool designed to measure and manage code complexity based on the principles of Cognitive-Driven Development (
CDD). It helps developers identify areas of the code that are difficult to understand and maintain by calculating the
Intrinsic Cognitive Point (ICP).

## Key Features

- **Multi-Language Support**: Analyzes both **Java** and **Kotlin** source code.
- **ICP Calculation**: Measures complexity based on branching logic, coupling, and exception handling.
- **SLOC Metrics**: Provides physical Source Lines of Code distribution.
- **Actionable Recommendations**: Suggests refactoring targets based on complexity thresholds.
- **Multiple Output Formats**: Supports Console, JSON, XML, and Markdown.

## HIGH PRIORITY

- **IF YOU DON'T CHECK SKILLS** your task will be invalidated and we will generate rework
- **YOU CAN ONLY** finish a task if the code is compiling and tests passed
- **NEVER** use web search tools to search local project code — for local code, use Grep/Glob instead

## Project Structure

This repository is a **composite Gradle workspace** with two included builds at the root:

- `icp-cli`
- `intellij-plugin`

Agents should think about this repository in two levels:

1. The repository root is mainly an orchestrator for the included builds.
2. Real compilation, testing, and most code changes happen inside `icp-cli` or `intellij-plugin`.

Current layout:

```text
.
├── settings.gradle.kts
├── icp-cli/
│   ├── core/
│   ├── languages/
│   │   ├── java/
│   │   └── kotlin/
│   └── cli/
└── intellij-plugin/
```

### `icp-cli`

`icp-cli` is the standalone analyzer application. It contains the reusable analysis engine plus the command-line entrypoint.

- `core`
  Shared engine and domain layer.
  Contains domain models, analyzer contracts, configuration loading, file scanning, aggregation, reporting, and shared
  utilities.
  It must remain parser-agnostic and must not depend on Java- or Kotlin-parser implementations.

- `languages/java`
  Java analyzer implementation.
  Contains Java-specific parsing and analysis logic, including Spoon-based integration.

- `languages/kotlin`
  Kotlin analyzer implementation.
  Contains Kotlin-specific parsing and analysis logic, including Kotlin compiler / PSI integration.

- `cli`
  Command-line composition root.
  Depends on `core`, `languages:java`, and `languages:kotlin`, wires the analyzers together, and exposes the executable CLI.

### `intellij-plugin`

`intellij-plugin` is a separate included build for the IntelliJ IDEA plugin.

- It depends on `icp-cli/core`.
- It does **not** own the core analysis engine.
- Changes in `icp-cli/core` can require verification in `intellij-plugin` because the plugin consumes that shared module.

### Dependency Direction

Agents should assume the dependency flow is:

```text
icp-cli/core
  ├── icp-cli/languages/java
  ├── icp-cli/languages/kotlin
  ├── icp-cli/cli
  └── intellij-plugin
```

Practical rule for agents:

- If a change stays inside one leaf module, verify only that module's build.
- If a change is made in a shared module such as `icp-cli/core`, also verify the modules that directly depend on it when the
  integration can be affected.

## Building the Application

To perform a full build of the application, run the following command from the project root:

```bash
./gradlew build
```

**MANDATORY FOR AGENTS**: After making code modifications, you MUST run verification only for the module you changed, plus
any module that depends on that changed module when the dependency is part of the exercised integration path.

Use these commands:

- Changes in `icp-cli/core`, `icp-cli/languages/java`, `icp-cli/languages/kotlin`, or `icp-cli/cli`:

  ```bash
  cd icp-cli && ./gradlew classes testClasses --quiet
  ```

- Changes in `intellij-plugin` only:

  ```bash
  cd intellij-plugin && ./gradlew classes testClasses --quiet
  ```

- Changes in `icp-cli/core` that affect the IntelliJ plugin integration:

  ```bash
  cd icp-cli && ./gradlew classes testClasses --quiet
  cd ../intellij-plugin && ./gradlew classes testClasses --quiet
  ```

Do not run module verification from the composite root unless the root itself gains matching tasks.

**MANDATORY FOR AGENTS BEFORE ANY REFACTORING IN KOTLIN FILES**: You MUST run the coverage task for the affected module
first.

Use these commands:

- Refactoring Kotlin in `icp-cli`:

  ```bash
  cd icp-cli && ./gradlew clean coverageReport
  ```

  Then inspect:

  ```text
  icp-cli/build/reports/jacoco/coverageReport/coverageReport.xml
  ```

- Refactoring Kotlin in `intellij-plugin`:

  ```bash
  cd intellij-plugin && ./gradlew clean koverXmlReport
  ```

  Then inspect:

  ```text
  intellij-plugin/build/reports/kover/report.xml
  ```

You MAY ONLY refactor code when the relevant module coverage report shows 100% coverage. If coverage is below 100%, you MUST
add or update tests first and MUST NOT refactor until coverage reaches 100% or ask the user permission explaining why 100%
coverage is not possible.

<critical>
# Coding Standards
You must follow these coding standards:

- Always use TDD (Test-Driven Development) before creating any new functionality. All test scenarios must be implemented
  first.
- Use camelCase for declaring methods, functions, and variables, PascalCase for classes and interfaces, and kebab-case for
  files and directories.
- Avoid abbreviations, but also do not use very long names (more than 25 characters).
- Declare constants to represent magic numbers for readability.
- Methods and functions must perform a clear, well-defined action, and this should be reflected in their name, which must
  always start with a verb, never a noun.
- Whenever possible, avoid passing more than 3 parameters; prefer using objects if necessary.
- Avoid side effects. In general, a method or function should perform either a mutation or a query, never allow a query to
  have side effects (mutation).
- Never nest more than two if/else statements; always prefer early returns.
- Never use flag parameters to switch method or function behavior; in such cases, extract them into separate methods or
  functions with specific behavior.
- Avoid long methods (more than 50 lines).
- Avoid large classes (more than 300 lines).
- Always invert dependencies for external resources in both use cases and interface adapters by applying the Dependency
  Inversion Principle.
- ONLY use comments to explain WHY not HOW
- Use Javadoc comments to explain the class or method's purpose when the name is not enough to explain it purposefully.
- Never declare more than one variable on the same line.
- Declare variables as close as possible to where they will be used.
- Prefer composition over inheritance whenever possible.
- Class member order:
    1. Primary-constructor properties
    2. Other properties
    3. init blocks
    4. Secondary constructors
    5. Public methods
    6. Private/internal helper methods near related public methods
    7. companion object

</critical>

## Anti-Patterns for Agents

**NEVER do these:**

1. **Skip skill activation** because "it's a small change" — every domain change requires its skill
2. **Activate only one skill** when the code touches multiple domains
3. **commit code** without user requesFting it

## Tests

**Method naming convention**: Use `back ticks`