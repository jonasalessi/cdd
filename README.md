# Cognitive-Driven Development (CDD)

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

This monorepo contains the tooling for **Cognitive-Driven Development (CDD)** — a methodology that measures and manages code complexity through the **Intrinsic Cognitive Point (ICP)** metric, helping teams identify code that is hard to understand and maintain.

> ### Foundations in Research
>
> This tooling is a direct implementation of the **Cognitive-Driven Development** methodology, following the theoretical
> framework established in:
>
> Tavares de Souza, A. L. O., Costa Pinto, V. H. S. 2020.  
> *Toward a Definition of Cognitive-Driven Development*, 2020 IEEE International Conference on Software Maintenance and
> Evolution (ICSME), pp. 776–778. https://doi.org/10.1109/ICSME46990.2020.00087

---

## Repository Layout

```text
.
├── icp-cli/          # Standalone CLI analyzer
└── intellij-plugin/  # IntelliJ IDEA plugin
```

---

## Projects

### `icp-cli` — Command-Line Analyzer

A standalone command-line tool that analyzes **Java** and **Kotlin** source code and reports ICP violations.

Key capabilities:
- ICP calculation based on branching, coupling, and exception handling
- Multiple output formats: console, JSON, XML, Markdown
- Flexible configuration via `.cdd.yaml`

[Read the full documentation](icp-cli/README.md)

---

### `intellij-plugin` — IntelliJ IDEA Plugin

An IntelliJ IDEA plugin that brings CDD analysis directly into the IDE, displaying ICP results as editor inlays and providing actions in the Project View and Editor menus.

[Read the full documentation](intellij-plugin/README.md)

---

## Building Everything

To build all modules from the repository root:

```bash
./gradlew build
```

For module-specific build instructions, refer to each project's README.

---

## License

This project is licensed under the Apache License 2.0 — see the [LICENSE](LICENSE) file for details.
