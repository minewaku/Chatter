---
name: rpi-exec-specs
description: Gather implementation specifications, then plan, clarify, execute, document, and synchronize the resulting change.
---
# Execute Specifications
Use this workflow when the user wants to describe a feature or change and have
Codex carry it from specification through implementation and handoff.
## Gather the Specification
Start by asking for the minimum decisions needed to prevent rework:
Desired user outcome and acceptance criteria.
Affected systems or repositories, including frontend/API implications.
Data, behavior, compatibility, security, and rollout constraints.
Ask these questions in turns using request_user_input: submit exactly one
concise, highest-priority question per call, with 2–3 mutually exclusive
options. Wait for the user's answer before submitting the next unanswered
question. Do not ask specification questions as normal text or bundle a
questionnaire. Do not assume a material product decision merely to continue.
Summarize facts already established before moving to planning. If
request_user_input is unavailable, state that structured clarification is
required and wait for the user rather than falling back to a prose question.
Do not ask again for facts already supplied. If a small ambiguity can be safely
resolved from the codebase, state the assumption in the plan; otherwise treat it
as a clarification blocker.
## Workflow
1. Inspect the current code, contracts, and documentation needed to ground the
work. Preserve unrelated local changes.
2. Write a dependency-aware checklist plan under docs/plans/, including
affected backend, frontend, API, migration, documentation, and verification
work as applicable.
3. Challenge the plan: surface ambiguous requirements, data and compatibility
risks, security concerns, external dependencies, and rollback implications.
Resolve blockers and obtain explicit implementation sign-off through exactly
one request_user_input call. Its recommended option must explicitly approve
the concrete implementation contract; a revise option must keep implementation
pending. Do not ask for sign-off as normal prose when request_user_input is
available.
4. Implement in small, verified checkpoints. Keep the source plan synchronized
and do not broaden scope without approval.
5. Update API contracts, frontend types/UI, localization, and team
documentation whenever the specification crosses those boundaries.
6. Run relevant validation. Clearly distinguish introduced failures from
pre-existing repository failures.
7. Complete the plan checklist, write a savepoint when the project uses them,
and give a concise implementation and frontend handoff.
## Non-Negotiable Checks
Do not begin implementation while clarification blockers remain unresolved.
Preserve repository-specific architecture and migration rules.
Treat explicit user scope as authoritative; do not convert an interface-only
request into a backend or data migration without approval.