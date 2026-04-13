---
name: skill-builder
description: Creates well-structured agent skills from a user's workflow or use case. Use when the user asks to build a skill, create a SKILL.md, design a reusable workflow, define skill triggers, or package instructions into a skill folder. Also use when the user wants to turn a conversation or repeated workflow into something reusable, says "make this a skill", asks about skill folder structure, or wants help writing agent instructions — even if they don't explicitly use the word "skill."
---

# Skill Builder

Create clear, portable, reusable skills from natural-language requests.

This skill transforms a user's workflow, domain knowledge, or repeated task into a proper skill package centered on `SKILL.md`, with optional `scripts/`, `references/`, and `assets/` folders when they earn their place.

## What this skill produces

Produce one or more of the following, depending on what the user asks for:

1. A complete `SKILL.md`
2. Suggested folder structure
3. Optional helper file recommendations for `scripts/`, `references/`, and `assets/`
4. Trigger phrases and description wording
5. Troubleshooting notes
6. A short packaging note for local use, upload, or API use

Start with the smallest useful artifact — don't generate unnecessary files.

## Core operating rules

- Always begin from concrete use cases, not vague abstractions — abstract skills lack the specificity needed for reliable triggering and clear instructions.
- Design for repeatable workflows, not one-off prompts — a skill that only runs once isn't worth the overhead of creating it.
- Keep the skill composable so it can coexist with other skills without conflicting triggers or overlapping scope.
- Keep the frontmatter precise, because the `description` field is what determines whether the skill gets loaded at all.
- Prefer progressive disclosure to keep context windows lean:
  - Put routing information in frontmatter (~100 words)
  - Put core instructions in the main body (<500 lines)
  - Put bulky or specialized material in `references/`
- Do not include `README.md` inside the skill folder — the `SKILL.md` already serves as both documentation and instructions, and a separate README creates confusion about which file is authoritative.
- Use kebab-case for the skill name and folder name.
- Never use XML-style angle brackets in frontmatter — YAML parsers can choke on them, silently breaking the skill's metadata.
- Do not invent external dependencies unless the user's use case actually needs them — every dependency is a potential failure point and makes the skill harder to share.

## When NOT to create a skill

Not every request is best served by a skill. Push back gently when:

- **The task is truly one-off** — if it won't repeat, a direct prompt is faster and better.
- **The request is a single instruction** — "format this JSON" doesn't need a skill; it needs a one-liner.
- **The scope is too vague to act on** — "a skill for everything about coding" can't produce useful instructions. Narrow it down first.
- **An existing skill already covers it** — check what's already available before duplicating.

## Workflow

### Step 1: Identify the skill target

Extract the user's real goal. For every requested skill, identify:

- The main outcome
- 2–3 concrete use cases
- Likely trigger phrases
- Whether the workflow is:
  - document or asset creation
  - workflow automation
  - MCP enhancement
- Whether the skill is problem-first or tool-first

If the request is vague, don't stall — your best guess at the repeatable workflow is usually close enough to get feedback. Draft something concrete and let the user steer from there.

### Step 2: Define the scope

Write a short internal scope summary before drafting:

- What the skill should do
- What it should not do
- What inputs it expects
- What outputs it should produce
- What environment assumptions exist
- Whether optional folders are needed

Use these heuristics for optional folders:

#### Use `scripts/` when:
- Deterministic validation is useful (parsing, transformation, file generation)
- The same helper logic would be reinvented every time the skill runs

#### Use `references/` when:
- Domain rules are too long for the main file (>50 lines of specialized content)
- API docs, schemas, or standards may be consulted on demand
- Examples would otherwise bloat `SKILL.md`

#### Use `assets/` when:
- Templates, sample files, visual assets, or reusable skeletons are part of the output

### Step 3: Write the frontmatter

Generate frontmatter that is specific enough to trigger correctly.

Rules:
- `name` must be kebab-case
- `description` must include what the skill does AND when to use it
- `description` should be "pushy" — agents tend to under-trigger skills, so err on the side of listing more trigger contexts rather than fewer
- Mention realistic user wording and relevant file types
- Keep it concise but actionable
- Only add `compatibility` if the skill has hard environment requirements (e.g., needs a specific MCP server)

### Step 4: Choose the instruction pattern

Pick the pattern that best fits the skill from these five options:

| Pattern | Use when | Example |
|---------|----------|---------|
| **A: Sequential workflow** | Fixed order, each step feeds the next | Build pipelines, document generation |
| **B: Multi-system coordination** | Spans multiple tools or services | API orchestration, MCP workflows |
| **C: Iterative refinement** | Quality improves through review cycles | Writing tasks, code generation |
| **D: Context-aware selection** | Same outcome, different paths | Framework-specific guides, deployment |
| **E: Domain-specific intelligence** | Value is embedded expertise or rules | Compliance checks, style enforcement |

If more than one pattern fits, combine them deliberately. See `references/patterns.md` for detailed structure of each pattern.

### Step 5: Draft the skill body

Use this structure unless the use case clearly needs something different:

```markdown
# [Skill Name]

One-paragraph summary of the skill's purpose and intended use.

## Use when
Bullet the situations, requests, or phrases that should activate the skill.

## Inputs
List the information, files, tools, or context the skill expects.

## Outputs
List the deliverables the skill should produce.

## Workflow
Provide the main step-by-step process.

## Decision rules
Provide branching logic, constraints, and fallback behavior.

## Quality bar
State what "good" looks like before finishing.

## Optional bundled resources
Point to references/, scripts/, or assets/ only when they exist.

## Examples
Include 1–3 realistic user requests and the expected behavior.
```

### Drafting guidelines

When writing instructions:

- Be explicit and operational — the model following this skill should never have to guess what "handle appropriately" means.
- Prefer concrete actions over generic advice.
- Name outputs clearly so the user knows exactly what to expect.
- Include validation steps and fallback behavior for when things go wrong.
- Explain the **why** behind important rules — a model that understands the reason will generalize better than one following a bare directive.
- Move long supporting material (>50 lines) to `references/` and point to it clearly.
- Keep the generated SKILL.md under 500 lines. If it's growing past that, it's a sign that reference material should be extracted into separate files.

### Quality bar

For every generated skill, verify before delivering:

- The goal is clear from the first paragraph
- The trigger conditions are specific and realistic
- The steps are executable, not just aspirational
- Outputs are concrete and named
- Failure handling exists for the most likely problems
- Optional resources are referenced with clear guidance on when to read them
- The scope is neither too vague ("help with code") nor too broad ("handle all DevOps")
- The description is "pushy" enough for reliable triggering
- Instructions explain "why" not just "what" for important rules
- The total SKILL.md is under 500 lines (or uses `references/` for overflow)

## Output format rules

When the user asks to create a skill, choose the appropriate mode:

### Minimal mode
Return only the folder name and `SKILL.md`. Use when the user asks for a quick draft.

### Standard mode (default)
Return the folder name, a purpose summary, the `SKILL.md`, and optional folder recommendations.

### Expanded mode
Return the folder name, purpose summary, `SKILL.md`, suggested `scripts/`, `references/`, `assets/` contents, packaging notes, and improvement suggestions. Use when the user asks for a production-ready or shareable skill.

## Skill generation procedure

When asked to make a new skill, follow this sequence:

1. Infer the repeated workflow
2. Define 2–3 concrete use cases
3. Identify likely trigger phrases
4. Choose the primary skill category
5. Choose the instruction pattern (see `references/patterns.md`)
6. Generate a compliant frontmatter block
7. Draft the body with explicit steps
8. Add troubleshooting
9. Add examples
10. Recommend optional folders only if justified
11. Check the output against the review checklist below
12. Return the final skill in the smallest useful package

## Review checklist

Before returning a skill, verify:

- [ ] The name is valid kebab-case
- [ ] The folder name matches the skill name
- [ ] `SKILL.md` is named exactly `SKILL.md`
- [ ] The description says both what it does and when to use it
- [ ] The description is pushy enough for reliable triggering
- [ ] Trigger phrases sound like something a user would really say
- [ ] The skill focuses on outcomes, not implementation trivia
- [ ] The workflow is stepwise and executable
- [ ] Instructions explain "why" for important constraints
- [ ] Large reference material is in `references/`, not stuffed into the main file
- [ ] The total SKILL.md is under 500 lines
- [ ] Examples use realistic user phrasing
- [ ] No XML-style angle brackets appear in frontmatter
- [ ] No `README.md` is included inside the skill folder

## Packaging notes

When asked how to package or share the result:

- Put everything in a folder named after the skill
- Include `SKILL.md` at the top level
- Add optional subfolders only when needed
- For upload workflows, the folder may be zipped if required
- Keep the skill portable unless platform-specific behavior is intentional

## Examples

### Example 1: Research workflow skill

**User says:** "Create a skill for competitive product research"

**Action:**
- Define use cases: market scan, competitor comparison, brief creation
- Choose Pattern C (iterative refinement)
- Generate frontmatter with trigger phrases like "analyze competitors", "market brief", "research this space"
- Produce a skill that structures intake, research method, synthesis, and final deliverables

### Example 2: Document generation skill

**User says:** "Make a skill that creates project proposals from notes"

**Action:**
- Classify as document and asset creation
- Define inputs: notes, constraints, target audience
- Define outputs: proposal draft, executive summary, risks section
- Include a quality bar for structure, clarity, and completeness

### Example 3: MCP-enhanced operational skill

**User says:** "Build a skill that creates sprint plans from Linear data"

**Action:**
- Classify as workflow automation + MCP enhancement
- Choose Pattern A (sequential workflow orchestration)
- Specify ordered steps: gather project status → assess capacity → prioritize work → generate sprint plan
- Include fallback guidance for missing data or connection issues

### Example 4: Quick skill with generated output

**User says:** "Make a skill for writing git commit messages in conventional format"

**Generated SKILL.md (abbreviated):**

```yaml
---
name: commit-message
description: Writes conventional commit messages from diffs or change descriptions. Use when the user asks to write a commit message, format a commit, wants conventional commits, or says something like "what should I commit this as."
---
```

```markdown
# Commit Message Writer

Generate well-formatted conventional commit messages from staged changes or natural-language descriptions.

## Use when
- The user asks to write or format a commit message
- The user has staged changes and wants a summary
- The user mentions "conventional commits" or commit formatting

## Inputs
- Git diff or staged changes (preferred)
- Natural-language description of changes (fallback)

## Workflow
1. Read the diff or description
2. Identify the change type (feat, fix, refactor, docs, test, chore)
3. Determine the scope from the affected files or modules
4. Write a concise subject line (<72 chars)
5. Add a body paragraph if the change is non-trivial

## Examples
**Input:** Added JWT authentication to the login endpoint
**Output:** `feat(auth): implement JWT-based authentication`
```