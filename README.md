# CDD CLI: Cognitive-Driven Development Analyzer

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

CDD CLI is a tool designed to measure and manage code complexity based on the principles of **Cognitive-Driven Development (CDD)**. It helps developers identify areas of the code that are difficult to understand and maintain by calculating the **Intrinsic Cognitive Point (ICP)**.

> [!IMPORTANT]
> ### 🎓 Foundations in Research
>
> This tool is a direct implementation of the **Cognitive-Driven Development (CDD)** methodology. We strictly follow the theoretical framework established in the seminal paper:
>
> **"Cognitive-Driven Development (CDD): A New Perspective on Code Complexity"**  
> *Furtado, F., et al. (2024). arXiv:2408.11209 [cs.SE]*
>
> [📖 **Read on ArXiv**](https://arxiv.org/abs/2408.11209) — [📥 **Download PDF**](https://arxiv.org/pdf/2408.11209)
>
> ---
> **Acknowledgements**  
> *We extend our deepest gratitude to the authors for quantifying the "invisible burden" of cognitive load, providing the software engineering community with a scientific path towards simpler, more maintainable code.*

## Key Features

- **Multi-Language Support**: Analyzes both **Java** and **Kotlin** source code.
- **ICP Calculation**: Measures complexity based on branching logic, coupling, and exception handling.
- **SLOC Metrics**: Provides physical Source Lines of Code distribution.
- **Actionable Recommendations**: Suggests refactoring targets based on complexity thresholds.
- **Native Performance**: Fast startup and low memory footprint via GraalVM Native Image.
- **Multiple Output Formats**: Supports Console (with colored output and charts), JSON, XML, and Markdown.

## Quick Start

### Prerequisites

- **Java**: JRE 21 or higher.
- **Native (Optional)**: Download the appropriate binary for your OS.

### Installation

Download the latest release from the [Releases](https://github.com/jonas/icp-cli/releases) page.

### Usage

Analyze a file or directory:

```bash
# Run with JAR
java -jar cdd-cli.jar /path/to/your/code

# Run with Native Binary
./cdd-cli /path/to/your/code
```

### Common Options

- `-l, --limit <double>`: Set the ICP limit per class (default: 10.0).
- `--sloc-limit <int>`: Set the SLOC limit for methods (default: 24).
- `--format <format>`: Output format (`console`, `json`, `xml`, `markdown`).
- `--output <file>`: Redirect output to a file.
- `--fail-on-violations`: Exit with code 1 if any class exceeds the ICP limit.

## Configuration

CDD CLI can be configured using a `.cdd.yaml` file in your project root or via the `--config` option.

```yaml
limit: 15.0
sloc:
  methodLimit: 30
internalCoupling:
  autoDetect: true
  packages: ["com.mycompany.service"]
reporting:
  format: "console"
  verbose: true
```

## Binary Size & Native Image

The native binary size (~110MB) is primarily attributed to the inclusion of the Kotlin compiler internals (IntelliJ Core), which are required for robust PSI-based analysis of Kotlin files. This allows the tool to run without a full JDK installed while maintaining high parse accuracy.

> [!NOTE]
> Currently, Kotlin analysis in the native binary has some resource discovery limitations. For full Kotlin support, the JAR-based distribution is recommended if you encounter issues.

## Contributing

We welcome contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for details.

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.