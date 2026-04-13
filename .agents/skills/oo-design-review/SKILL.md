---
name: oo-design-review
description: Reviews Object-Oriented code against SOLID and design principles (SRP, DIP, OCP, Encapsulation, LSP, ISP, Consistency, Design Smells) and produces actionable refactoring recommendations. Use when the user asks to review OO design, check SOLID principles, find design smells, apply design principles to code, or validate a class against OO best practices. Do not use for non-OO languages, performance profiling, or automated refactoring.
metadata:
  author: Jonas Alessi
  version: 1.0.0
  category: code-review
---

# OO Design Review

Reviews Object-Oriented source code (Java, Kotlin, or any OO language) against eight battle-tested design principles derived from Cognitive-Driven Development practice. The skill identifies violations, explains why they matter, and suggests concrete refactoring steps with before/after examples.

## Use when

- The user asks to "review OO design", "check SOLID principles", "find design smells", or "apply design principles"
- The user shares a class, module, or file and asks for design feedback
- The user wants to validate code changes against OO best practices before merging
- The user asks "is this class well-designed?" or "how can I improve this code's design?"
- During code review when structural quality is in question

## Do not use when

- The code is in a non-OO paradigm (purely functional, scripting)
- The user is asking about performance, security, or infrastructure concerns unrelated to OO design
- The user wants automated refactoring applied directly (this skill only recommends)

## Inputs

- One or more source code files, classes, or modules to review
- Optionally, a specific concern or principle the user wants to focus on

## Outputs

A structured review document containing:

1. **Summary** — one-paragraph overall assessment
2. **Findings table** — each finding with: principle violated, severity (high/medium/low), location (file:line), description, and suggested fix
3. **Detailed recommendations** — for each high/medium finding, a before/after code sketch showing the recommended refactoring
4. **Praise section** — highlight what is already well-designed (positive reinforcement)

## Workflow

### Step 1: Collect the code to review

Read all files the user has provided or referenced. If the user pointed to a directory, scan for OO source files (`.java`, `.kt`, `.scala`, `.cs`, `.ts`, etc.).

### Step 2: Read the principles catalog

Open the reference file at `references/principles-catalog.md` within this skill folder. This contains the 8 principles with violation signals and corrective patterns.

### Step 3: Analyze each class against all 8 principles

For every class or significant unit in the input, check against these principles in order:

| ID | Principle | Key question |
|----|-----------|-------------|
| P1 | SRP / Cohesion | Does this class have only one reason to change? |
| P2 | DIP / Coupling | Does it depend on abstractions or concrete implementations? |
| P3 | OCP | Can behavior be extended without modifying this class? |
| P4 | Encapsulation | Does it hide its internals and follow Tell Don't Ask? |
| P5 | LSP / Composition | If inheritance is used, does the subclass honor the parent contract? |
| P6 | ISP | Are interfaces thin and cohesive? Are method parameters narrow? |
| P7 | Consistency | Can objects exist in invalid states? Are constructors rich enough? |
| P8 | Design Smells | Is there Feature Envy or other code smell? |

### Step 4: Classify findings

For each violation found:

- **High severity** — the violation actively causes maintenance burden, fragility, or bugs
- **Medium severity** — the violation will cause problems as the codebase grows
- **Low severity** — minor improvement opportunity, not urgent

### Step 5: Draft refactoring suggestions

For each high and medium finding, write a concrete before/after code sketch showing how to fix the violation. Reference the specific principle and its corrective pattern from the catalog.

### Step 6: Compile the review document

Produce the final output with all sections listed under Outputs above.

## Decision rules

- If the user specifies a single principle to focus on, only report findings for that principle but still scan all code.
- If no violations are found, explicitly state that the code adheres well to OO principles and highlight the good patterns observed.
- If the reviewed code is in a language the agent is less familiar with, use the violation signals as heuristics and note any uncertainty.
- If a class violates multiple principles, list each violation separately rather than combining them.
- When a violation overlaps multiple principles (e.g., Feature Envy involves both P4 and P8), report it under the primary principle and cross-reference the secondary one.

## Quality bar

Before finishing the review, verify:

- Every finding references a specific principle ID (P1–P8)
- Every high/medium finding includes a concrete refactoring suggestion with code
- The review covers all provided files, not just the first one
- The praise section exists and is not empty (there is always something good)
- Severity assignments are consistent: similar-scale issues have similar severities
- The summary accurately reflects the overall state of the code

## Bundled resources

- `references/principles-catalog.md` — Full principle definitions, violation signals, corrective patterns, and bad/good code examples for all 8 principles

## Examples

### Example 1: Full class review

**User says:** "Review this class for OO design issues"

**Agent behavior:**
1. Reads the provided class
2. Opens `references/principles-catalog.md`
3. Checks the class against all 8 principles
4. Finds: SRP violation (branching on type), DIP violation (concrete instantiation), Feature Envy
5. Produces a findings table with severity, a refactoring sketch for each, and praise for what is done well

### Example 2: Focused review on a specific principle

**User says:** "Does this code follow the Open-Closed Principle?"

**Agent behavior:**
1. Reads the provided code
2. Opens `references/principles-catalog.md`, focuses on P3
3. Checks if dependencies are injected or hardcoded
4. Reports whether OCP is satisfied or violated, with concrete evidence

### Example 3: Pre-merge validation

**User says:** "Check if my refactoring follows SOLID before I merge"

**Agent behavior:**
1. Reads the changed files
2. Checks all 8 principles on the new code
3. Compares against common anti-patterns from the catalog
4. Produces a pass/fail summary per principle with detailed notes on any remaining issues
