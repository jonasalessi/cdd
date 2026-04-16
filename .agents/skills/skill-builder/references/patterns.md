# Instruction Patterns

Choose the pattern that best fits the skill being created. If more than one fits, combine them deliberately instead of mixing loosely.

## Pattern A: Sequential Workflow Orchestration

Use when the task has a fixed order — each step feeds the next, and skipping ahead breaks things.

Structure:
1. Intake — gather inputs and validate them
2. Preparation — set up context, resolve dependencies
3. Execution — perform the core work
4. Validation — check the output against the quality bar
5. Final output — deliver the result

**Good for:** build pipelines, document generation from templates, data migration scripts.

## Pattern B: Multi-System Coordination

Use when the skill spans multiple tools, APIs, or services and needs to bridge between them.

Structure:
1. Gather context from the primary source
2. Call system A
3. Transform output into the format system B expects
4. Call system B
5. Consolidate results into a unified deliverable

**Good for:** MCP-enhanced workflows, cross-platform integrations, API orchestration.

## Pattern C: Iterative Refinement

Use when quality improves through review cycles and the first draft is rarely good enough.

Structure:
1. Produce an initial draft
2. Check against defined quality criteria
3. Revise the weakest areas
4. Repeat until the quality bar is met
5. Finalize and deliver

**Good for:** writing tasks, design reviews, code generation with quality constraints.

## Pattern D: Context-Aware Selection

Use when the same outcome may require different paths depending on the user's situation.

Structure:
1. Inspect the user's context (files, environment, request phrasing)
2. Choose the best route from available options
3. Explain the selection when it isn't obvious
4. Execute the chosen path
5. Provide fallback guidance if the chosen path is blocked

**Good for:** deployment skills (pick AWS vs GCP), framework-specific guides, environment-aware tooling.

## Pattern E: Domain-Specific Intelligence

Use when the skill's value comes from embedded expertise, rules, or policies that the model wouldn't naturally apply.

Structure:
1. Collect relevant facts from the user's input
2. Apply domain rules and constraints
3. Gate or adjust actions based on policy
4. Document reasoning in terms the user can follow
5. Produce a governed, compliant output

**Good for:** compliance checks, style guides, regulated workflows, coding standards enforcement.
