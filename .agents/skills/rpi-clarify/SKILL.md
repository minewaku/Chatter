---
name: rpi-clarify
description: Use when the user wants to review an approved plan, surface open questions and risks, and get final sign-off before implementation begins.
---

# RPI Clarify

## Purpose
Review a plan for completeness, surface every open question and risk, and produce a cleared plan that is safe to implement.

## Use This Skill When
- A plan exists and needs review before code changes begin.
- The user wants to challenge assumptions, catch blind spots, or resolve ambiguities.
- There are open questions, risks, or cross-cutting concerns that could derail implementation.
- The user wants explicit sign-off on "ready to build" before handing off to `rpi-implement`.

## Skip Condition
- If the plan was already clarified in a prior session and the user says "proceed" or "implement", skip directly to `rpi-implement`.
- If the plan is trivial (single file, single step, no dependencies, no ambiguity), clarify inline rather than invoking this skill.

## Initial Response
When invoked, respond with:

```text
I'm ready to review the plan before implementation. I'll surface open questions, risks, and assumptions — then we'll resolve or acknowledge each one before green-lighting the build.
```

Then proceed with the workflow.

## Workflow
1. **Read the plan in full.** Load the plan file from `docs/plans/` and any referenced research or `AGENTS.md`.
2. **Extract every assumption.** Scan the plan for implicit or explicit assumptions — things the plan treats as true without verification.
3. **Surface open questions.** For each step, ask:
   - Is the input (files, APIs, data shapes) well-understood?
   - Are there ambiguous requirements or naming collisions?
   - Does the step depend on something outside the plan's control?
4. **Identify risks.** For each step, assess:
   - **Blast radius** — if this step goes wrong, what breaks?
   - **Reversibility** — can it be undone cleanly?
   - **Dependency risk** — does it rely on external services, migrations, or team actions?
   - **Edge cases** — what inputs or states could cause failures?
5. **Check cross-cutting concerns.** Look for:
   - Performance implications
   - Security considerations
   - Data migration or schema changes
   - Backward compatibility
   - Observability (logging, metrics, error handling)
6. **Classify each finding.** Tag as:
   - 🔴 **Blocker** — must resolve before implementation starts
   - 🟡 **Risk** — acknowledged risk with mitigation or acceptance noted
   - 🟢 **Clarified** — question resolved, no action needed
7. **Present the review to the user.** Output the structured review (see Output Shape).
8. **Resolve blockers.** Work through each 🔴 item with the user until it is resolved or descoped.
9. **Get explicit sign-off.** Ask: "All blockers resolved, risks acknowledged. Ready to implement?" — wait for user confirmation.
10. **Update the plan file.** Write back to `docs/plans/` with:
    - Clarified assumptions
    - Resolved questions
    - Acknowledged risks with mitigations
    - A `## Clarification Sign-Off` section with date and confirmation

## Output Shape
- **Goal** — what this plan achieves
- **Assumptions** — explicit and implicit, each tagged
- **Open Questions** — each with status (🔴/🟡/🟢) and resolution
- **Risks** — each with severity, blast radius, and mitigation
- **Cross-Cutting Concerns** — performance, security, compatibility, etc.
- **Clarified Plan** — the plan with resolved questions and acknowledged risks folded in
- **Sign-Off** — explicit user confirmation to proceed

## Disk Persistence
- Update the existing plan file in `docs/plans/` with the clarification results.
- If the plan file doesn't exist yet, create it with the naming convention: `xxx-plan.md`.
- The plan file should now contain a `## Clarification` section documenting:
  - Questions resolved
  - Risks acknowledged
  - Sign-off status and date

## Compaction Rule
Compaction is mandatory.
- Compact when context is approaching 45% of usable capacity.
- Compact after reviewing each major section of the plan, after resolving a batch of questions, and before presenting the final review.
- Keep only:
  - Objective
  - Findings so far (questions, risks, concerns)
  - Items still to review
  - Next action

## Guardrails
- This phase produces decisions, not code. Do not start implementation.
- Prefer surfacing concerns early over discovering them mid-implementation.
- If a blocker cannot be resolved, flag it clearly and propose descoping or reordering rather than silently proceeding.
- The goal is a **cleared plan** — every open question answered or explicitly deferred, every risk acknowledged.
