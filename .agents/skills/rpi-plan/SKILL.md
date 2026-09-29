---
name: rpi-plan
description: Use when the user wants to turn research into a concrete implementation plan with sequencing, dependencies, and verification.
---

# RPI Plan

## Purpose
Turn research findings into a small, ordered, reviewable plan.

## Use This Skill When
- Research is complete and the next step is planning.
- The user wants an implementation sequence before code changes begin.
- The task needs dependencies, file ownership, risks, or validation defined.

## Workflow
1. Start from the research findings, not a blank slate.
2. Read any referenced files or plans in full.
3. Break the work into small, dependency-aware steps.
4. Identify which files or subsystems each step touches.
5. State what must be verified before moving on.
6. Write the plan as a checklist so progress can be tracked and updated during implementation.

## Output Shape
- Goal
- Assumptions and constraints
- Step-by-step plan with checklist items
- Files likely to change
- Verification plan
- Risks or open questions

## Disk Persistence
Always persist plan files to disk under `docs/plans/` folder with naming convention: `xxx-plan.md`

Example: `plans/image-upload-library-plan.md`

This ensures plans are:
- Reviewable in version control
- Accessible outside the conversation
- Trackable across sessions
- Documented for the team

## Compaction Rule
Compaction is mandatory.
- Compact when context is approaching 45% of usable capacity.
- Compact after every major decision, after adding a new branch of the plan, and before handing off to implementation.
- Keep only:
  - Objective
  - Decisions made
  - Constraints
  - Files likely to change
  - Unknowns
  - Next action

## Guardrails
- Keep steps atomic and verifiable.
- Do not start editing code in this phase unless the user explicitly asks.
- Prefer sequencing that reduces rework and preserves reviewability.
