---
description: Review the CLI and configuration sources and update the documentation accordingly. The README must accurately reflect the current codebase.
---

Review the CLI and configuration sources and update the documentation accordingly. The README must accurately reflect the current codebase.

1. Update CLI Options Documentation

“The README must match the actual CLI inputs supported by the code.”

	•	Inspect CddCli.kt to identify all supported command-line options.
	•	Update the README.md section ### Common Options to include every option found.
	•	Use the following format for each option:

- `-l, --limit <double>`: Set the ICP limit per class (default: 10.0).


	•	Ensure:
	•	Short and long flags are both documented (if available).
	•	Parameter types are explicit.
	•	Default values are included when applicable.
	•	Descriptions are concise and accurate.

⸻

2. Update YAML Configuration Documentation

“Every configurable property must be documented, with no exceptions.”

	• Inspect all .kt files under:

src/main/kotlin/com/cdd/core/config


	• Update the README.md section ## Configuration to document all supported YAML inputs.
	• For each configuration property:
	  • List the property name exactly as used in YAML.
	  • Add an inline comment explaining what it does.
	  • Do not omit any property found in the code.
          • Add the information if it optional or not, if optional then add the default value it uses
	  • Example format:

icp:
  limit: 10.0 # Maximum ICP value allowed per class
  enabled: true # Enables or disables ICP validation


⸻

3. Quality Bar (Non-Negotiable)

“If it exists in code, it must exist in the README.”

	• No undocumented options.
	• No outdated descriptions.
	• No vague explanations.
	• Documentation must reflect current behavior, not assumptions.