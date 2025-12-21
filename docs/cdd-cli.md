# CDD CLI Technical Reference

This document provides a comprehensive guide to all features, options, and configuration parameters of the CDD CLI.

## Analysis Logic

[!NOTE]
> **Research Foundation**
> This tool implements the **Cognitive-Driven Development (CDD)** methodology as defined in:
> *Furtado, F., et al. (2024). "Cognitive-Driven Development (CDD): A New Perspective on Code Complexity". arXiv:2408.11209.*
> [📖 **Read on ArXiv**](https://arxiv.org/abs/2408.11209)

CDD CLI calculates the **Intrinsic Cognitive Point (ICP)** for classes and methods. ICP is an additive metric that quantifies the cognitive load required to understand a piece of code.

### Complexity Factors

| Factor | Weight (Default) | Description |
| :--- | :--- | :--- |
| **Code Branch** | 1.0 | `if`, `when`, `for`, `while`, `try`, `catch`, `safe calls`. |
| **Condition** | 0.5 | Boolean operators (`&&`, `||`, `!`) and conditional expressions. |
| **Exception Handling** | 1.0 | `try`, `catch`, `finally` blocks and `throw` statements. |
| **External Coupling** | 1.0 | Usage of classes/types outside of your project and core libraries (JDK/Kotlin stdlib). |
| **Internal Coupling** | 0.5 | Usage of classes/types defined within your internal project packages. |

## Command-Line Interface

### Syntax

```bash
cdd-cli [OPTIONS] [PATH]
```

### Options

| Option | Shorthand | Description | Default |
| :--- | :--- | :--- | :--- |
| `--limit` | `-l` | The maximum allowable ICP per class. | `10.0` |
| `--sloc-limit` | | The maximum Source Lines of Code for a method. | `24` |
| `--format` | | Output format: `console`, `json`, `xml`, `markdown`. | `console` |
| `--output` | | Path to save the report. | Stdout |
| `--config` | | Path to a custom configuration file. | `.cdd.yaml` |
| `--include` | | Glob patterns for files to include. | `**/*.{kt,kts,java}` |
| `--exclude` | | Glob patterns for files to ignore. | `**/build/**`, `**/test/**` |
| `--fail-on-violations`| | Exit with code 1 if violations are found. | `false` |
| `--verbose` | `-v` | Enable detailed logging. | `false` |
| `--version` | | Show version information. | - |

## Configuration File

The `.cdd.yaml` file allows you to define project-specific thresholds and package logic.

### Full Example

```yaml
# Strictness Thresholds
limit: 12.0
sloc:
  methodLimit: 25

# Coupling Detection
internalCoupling:
  autoDetect: true
  packages:
    - "com.cdd.core"
    - "com.cdd.domain"

# Analysis Scope
include:
  - "src/main/kotlin/**/*.kt"
  - "src/main/java/**/*.java"
exclude:
  - "**/generated/**"

# Weight Overrides (Experimental)
# You can customize the ICP weights for each type
icpTypes:
  CODE_BRANCH: 1.5
  CONDITION: 0.2
  EXTERNAL_COUPLING: 2.0

# Reporter Settings
reporting:
  format: "markdown"
  outputFile: "build/reports/cdd.md"
  verbose: false
```

## Known Limitations

### Kotlin Analysis in Native Binary
The native binary may occasionally fail to resolve certain Kotlin compiler classpath resources due to the way `kotlin-compiler-embeddable` interacts with the GraalVM image heap. If you see "Resource not found" errors, use the JAR distribution.

### Non-Standard Code Patterns
Highly dynamic code or code using heavy reflection might not have its coupling fully detected, as the analysis is primarily static and PSI-based.

## Support

For issues, please visit the [Issue Tracker](https://github.com/jonasalessi/cdd-cli/issues).
