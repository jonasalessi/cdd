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

## Project Structure

This project is organized into four Gradle modules:

- **`core`**: Contains the reusable CDD engine, including domain models, analyzer contracts, configuration loading, file
  scanning, aggregation, reporting, and shared utilities. It does not contain parser-specific Java or Kotlin dependencies.
- **`languages:java`**: Contains the Java-specific analyzer implementation and Spoon-based parsing logic.
- **`languages:kotlin`**: Contains the Kotlin-specific analyzer implementation and Kotlin compiler/PSI-based parsing logic.
- **`cli`**: Contains the command-line interface built with Clikt. It acts as the composition root by depending on `core`,
  `languages:java`, and `languages:kotlin`, then registering the concrete analyzers for execution.

Current top-level layout:

```text
core/
languages/
  java/
  kotlin/
cli/
```

## Building the Application

To perform a full build of the application, run the following command from the project root:

```bash
./gradlew build
```

**MANDATORY FOR AGENTS**: After making ANY code modifications, you MUST ALWAYS run the following command

```bash
./gradlew classes testClasses --quiet
```

**MANDATORY FOR AGENTS BEFORE ANY REFACTORING IN Kotlin FILES**: You MUST ALWAYS run the coverage task first.

```bash
./gradlew clean coverageReport
```

Then you MUST inspect the aggregate coverage report at:

```text
build/reports/jacoco/coverageReport/coverageReport.xml
```

Use the following script to inspect the coverage counters for the Kotlin file you plan to refactor. Replace
`TARGET_CLASS` with the JaCoCo class name, for example `com/cdd/analyzer/java/JavaAnalyzer`.

```bash
TARGET_CLASS="com/cdd/analyzer/java/JavaAnalyzer" python3 - <<'PY'
import os
import xml.etree.ElementTree as ET

target_class = os.environ["TARGET_CLASS"]
root = ET.parse("build/reports/jacoco/coverageReport/coverageReport.xml").getroot()

for package in root.findall("package"):
    for current_class in package.findall("class"):
        if current_class.get("name") == target_class:
            print("CLASS", current_class.get("name"))
            for counter in current_class.findall("counter"):
                print(counter.get("type"), "missed=", counter.get("missed"), "covered=", counter.get("covered"))

            source_file = package.find(f"sourcefile[@name='{target_class.rsplit('/', 1)[-1]}.kt']")
            if source_file is not None:
                print("SOURCEFILE", source_file.get("name"))
                for counter in source_file.findall("counter"):
                    print("SF", counter.get("type"), "missed=", counter.get("missed"), "covered=", counter.get("covered"))
            raise SystemExit(0)

print("NOT_FOUND")
raise SystemExit(1)
PY
```

You MAY ONLY refactor code when the coverage report shows 100% coverage. If coverage is below 100%, you MUST add or update
tests first and MUST NOT refactor until coverage reaches 100%.

# Coding Standards

	•	Always use TDD (Test-Driven Development) before creating any new functionality. All test scenarios must be implemented first.
	•	Use camelCase for declaring methods, functions, and variables, PascalCase for classes and interfaces, and kebab-case for files and directories.
	•	Avoid abbreviations, but also do not use very long names (more than 25 characters).
	•	Declare constants to represent magic numbers for readability.
	•	Methods and functions must perform a clear, well-defined action, and this should be reflected in their name, which must always start with a verb, never a noun.
	•	Whenever possible, avoid passing more than 3 parameters; prefer using objects if necessary.
	•	Avoid side effects. In general, a method or function should perform either a mutation or a query, never allow a query to have side effects (mutation).
	•	Never nest more than two if/else statements; always prefer early returns.
	•	Never use flag parameters to switch method or function behavior; in such cases, extract them into separate methods or functions with specific behavior.
	•	Avoid long methods (more than 50 lines).
	•	Avoid large classes (more than 300 lines).
	•	Always invert dependencies for external resources in both use cases and interface adapters by applying the Dependency Inversion Principle.
	•	ONLY use comments to explain WHY not HOW
	•	Use Javadoc comments to explain the class or method's purpose when the name is not enough to explain it purposefully.
	•	Never declare more than one variable on the same line.
	•	Declare variables as close as possible to where they will be used.
	•	Prefer composition over inheritance whenever possible.
 
