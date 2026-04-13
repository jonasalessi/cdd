---
name: oo-design-guide
description: Guides developers in building well-designed Object-Oriented code from scratch by applying SOLID and OO design principles (SRP, DIP, OCP, Encapsulation, LSP, ISP, Consistency, Feature Envy avoidance) during active development. Use when the user asks to design a class, model a domain, structure a feature, architect OO code, or wants help applying design principles while writing new code. Do not use for reviewing existing code (use oo-design-review instead), non-OO languages, performance tuning, or infrastructure decisions.
metadata:
  author: Jonas Alessi
  version: 1.0.0
  category: design-guidance
---

# OO Design Guide

Guides developers through building Object-Oriented code correctly from the start by applying eight proven design principles at every decision point. Unlike the review skill (which inspects existing code), this skill acts as a design companion during active development — shaping class structure, interface boundaries, dependency flow, and object construction as the code is being written.

## Use when

- The user asks "how should I design this class?" or "help me model this domain"
- The user is starting a new feature and wants to structure it with good OO design
- The user says "build this following OO principles" or "structure this feature"
- The user asks "what interfaces do I need?" or "how should I split this?"
- The user wants to model domain concepts into classes, interfaces, and relationships
- The user is designing a class hierarchy and wants guidance on inheritance vs composition
- During TDD, when the user needs help designing the production class before writing it

## Do not use when

- The user wants to review already-written code for violations (use `oo-design-review` instead)
- The code is in a non-OO paradigm
- The user is asking about performance, deployment, or infrastructure concerns
- The request is about fixing a bug, not about design structure

## Inputs

- A description of the feature, domain, or behavior to implement
- Optionally, existing classes or interfaces that the new code must integrate with
- Optionally, a specific principle the user wants to focus on

## Outputs

A design proposal containing:

1. **Domain model** — the key domain concepts, their responsibilities, and relationships
2. **Class design** — proposed class names, their single responsibility, constructor signatures, and public methods
3. **Interface definitions** — abstractions extracted to support DIP and OCP
4. **Dependency map** — which class depends on which interface, and how they are wired
5. **Design rationale** — for each structural decision, which principle(s) it satisfies and why
6. **Implementation skeleton** — code scaffolding that the developer can fill in

## Workflow

### Step 1: Understand the domain

Gather the feature requirements from the user. Identify:

- What are the core domain concepts (nouns)?
- What are the behaviors (verbs)?
- What are the variations (different rules, types, or categories)?
- What are the external dependencies (database, APIs, messaging)?

If the requirements are unclear, ask clarifying questions before proceeding.

### Step 2: Load the principles catalog

Open `references/principles-catalog.md` within this skill folder. This contains the 8 design principles with design questions and patterns to follow.

### Step 3: Define responsibilities (apply P1 — SRP)

For each domain concept, assign a single, clear responsibility. Ask:

- "If I described what this class does, would I use the word 'and'?"
- If yes, split it into separate classes.

**Output:** A list of classes, each with one sentence describing its single responsibility.

### Step 4: Design abstractions (apply P2 — DIP and P3 — OCP)

For each external dependency or behavioral variation:

1. Define an interface that represents the contract
2. Place the interface in the domain/core layer
3. Place the concrete implementation in the infrastructure/adapter layer

Ask:
- "Can I test this class without real infrastructure?"
- "Can I extend behavior without modifying this class?"

**Output:** Interface definitions and their relationship to concrete implementations.

### Step 5: Design object construction (apply P7 — Rich Constructors)

For each class, design the constructor:

1. List all mandatory attributes — these go in the constructor
2. Remove no-arg constructors that would allow invalid state
3. Consider tiny types (value objects) for domain primitives

Ask:
- "Can someone create an invalid instance of this class?"
- "Are there primitive types that deserve their own value object?"

**Output:** Constructor signatures with required parameters.

### Step 6: Design class interactions (apply P4 — Encapsulation and P8 — Feature Envy)

For each behavior that spans multiple classes:

1. Identify which class owns the data the behavior operates on
2. Place the behavior method in the owning class
3. External callers invoke a single meaningful method

Ask:
- "Is this method calling getters on another object to make a decision?"
- "Would this logic be simpler if it lived inside the other class?"

**Output:** Method signatures showing where behavior lives and how classes interact.

### Step 7: Evaluate inheritance decisions (apply P5 — LSP and Composition)

For each proposed inheritance relationship:

1. Verify it is a strict "is-a" relationship
2. Check that every subclass can fulfill all parent methods without exceptions
3. If in doubt, use composition instead

Ask:
- "Would any subclass need to throw UnsupportedOperationException?"
- "Am I using inheritance just to reuse code?"

**Output:** Decision on inheritance vs composition for each case, with rationale.

### Step 8: Trim interfaces (apply P6 — ISP)

For each interface:

1. Check that every implementer uses all methods
2. Split fat interfaces into smaller, cohesive contracts
3. Ensure method parameters use the narrowest possible type

Ask:
- "Would any implementer leave a method unimplemented?"
- "Am I passing a large object when only one field is needed?"

**Output:** Final interface definitions, potentially split from the originals.

### Step 9: Produce the design proposal

Compile all outputs from Steps 3–8 into a structured design proposal. Include:

1. Domain model overview
2. Class list with responsibilities
3. Interface definitions
4. Dependency map
5. Constructor signatures
6. Method placement rationale
7. Implementation skeleton (compilable but empty method bodies)

### Step 10: Validate the design

Run a final check against all 8 principles:

| Check | Question | Pass? |
|-------|----------|-------|
| P1 | Does every class have exactly one reason to change? | |
| P2 | Do all classes depend on abstractions, not concretions? | |
| P3 | Can behavior be extended without modifying existing classes? | |
| P4 | Do all classes follow Tell Don't Ask? | |
| P5 | Is inheritance used only for true "is-a" and never breaks contracts? | |
| P6 | Are all interfaces thin and cohesive? | |
| P7 | Can any object exist in an invalid state? | |
| P8 | Is there any Feature Envy? | |

If any check fails, revise the design before presenting it to the user.

## Decision rules

- If the user provides a specific principle to focus on, still apply all 8 but emphasize the requested one in the rationale.
- If the domain is too vague to design, ask the user up to 3 clarifying questions before proceeding. Do not guess blindly.
- If the user already has existing code to integrate with, read it first and design the new code to complement it — do not propose rewriting what already works.
- If the feature is small (1–2 classes), skip the domain model overview and go straight to class design.
- When generating the implementation skeleton, use the same language the user is working in. Default to Kotlin if unspecified.
- Always present the design rationale — the user should understand WHY each decision was made, not just WHAT was decided.

## Quality bar

Before presenting the design, verify:

- Every class has a single, clearly stated responsibility
- Every dependency flows through an interface, not a concrete class
- Every constructor enforces valid state — no half-initialized objects
- No method exhibits Feature Envy
- No interface forces implementers to throw UnsupportedOperationException
- The design rationale references specific principle IDs (P1–P8)
- The implementation skeleton compiles (or would compile with filled method bodies)
- The proposal is sized appropriately — not over-engineered for a simple feature

## Bundled resources

- `references/principles-catalog.md` — The 8 design principles framed as design questions and patterns to follow during active development

## Examples

### Example 1: New feature design

**User says:** "I need to build an invoice processing system. Help me design the classes."

**Agent behavior:**
1. Asks clarifying questions: What types of invoices? What external systems (email, database)? What business rules?
2. Loads the principles catalog
3. Identifies domain concepts: Invoice, Payment, TaxCalculator, InvoiceRepository
4. Designs each class with SRP, creates interfaces for TaxCalculator and InvoiceRepository (DIP/OCP)
5. Designs rich constructors for Invoice (must have customer and at least one line item)
6. Places payment logic inside Invoice (encapsulation, no Feature Envy)
7. Produces a class diagram, interface definitions, and implementation skeleton

### Example 2: Modeling a domain concept

**User says:** "How should I model different discount strategies for our e-commerce?"

**Agent behavior:**
1. Recognizes this as a SRP + OCP scenario (multiple variants of a behavior)
2. Proposes a `DiscountStrategy` interface with a `calculate(price: Double): Double` method
3. Designs individual implementations: `PercentageDiscount`, `FixedAmountDiscount`, `TieredDiscount`
4. Shows how the `PriceCalculator` accepts a `DiscountStrategy` via constructor injection
5. Explains: "This follows P1 (each strategy has one rule), P2/P3 (injected interface, open for extension)"

### Example 3: Constructor design help

**User says:** "My Order class has too many optional fields, how should I design the constructor?"

**Agent behavior:**
1. Reads the existing Order class
2. Separates mandatory fields (customer, date) from truly optional ones
3. Proposes a rich constructor with mandatory fields + a builder pattern for optional fields
4. Suggests tiny types for domain primitives (OrderId, Money)
5. Validates: "With this design, P7 is satisfied — no Order can exist without a customer"
