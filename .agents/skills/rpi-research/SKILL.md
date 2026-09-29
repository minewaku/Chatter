---
name: rpi-research
description: Use when you need a one-time project blueprint and architecture map for a codebase, written into AGENTS.md so future sessions inherit the context.
---

# RPI Research

## Purpose
Create a durable project blueprint from the live codebase and store it in `AGENTS.md` so future sessions can reuse the context.

## Use This Skill When
- You are entering a new project and need the codebase mapped once.
- You want a project blueprint, architecture summary, and file inventory.
- You want the durable context captured in `AGENTS.md`, not only in chat.
- You want to understand request flow, data flow, and component relationships without proposing changes.

## Skip Condition
If the repo already has `AGENTS.md` or an existing blueprint file, skip the one-time blueprint creation phase.
- Read the existing file first.
- Reuse it as the durable project context.
- Only do fresh research if the user explicitly asks to update or replace that blueprint.

## Initial Response
When invoked, respond with:

```text
I'm ready to research the codebase and create a durable project blueprint. Please provide the project area or question, and I'll document the current architecture and save the result in AGENTS.md for future sessions.
```

Then wait for the user's research question.

## Workflow
1. Read `AGENTS.md` or the existing blueprint file first, if it exists.
2. If no durable blueprint exists yet, read any directly mentioned files in full.
3. Identify the main entrypoints, config, models, handlers, services, workers, storage, and repositories.
4. Trace one or two primary flows end to end.
5. Record file paths and line references for the important pieces.
6. Synthesize a project blueprint that describes what exists, where it lives, and how it fits together.
7. Write or update the repo-root `AGENTS.md` with the blueprint only when no durable blueprint already exists.

## AGENTS.md Content
The `AGENTS.md` file should capture the durable project context, including:
- project purpose
- main entrypoints
- core components and responsibilities
- information flow
- important constraints and dependencies
- open questions that remain relevant

## Output Shape
- Summary
- Files and responsibilities
- Information flow
- Existing constraints and dependencies
- Project blueprint written to `AGENTS.md`
- Open questions

## Compaction Rule
Compaction is mandatory.
- Compact when context is approaching 45% of usable capacity.
- Compact after each major file sweep, before starting a new subsystem, and before handing off the blueprint to `AGENTS.md`.
- Keep only:
  - Goal
  - Confirmed facts
  - Files inspected
  - Open questions
  - Next step

## Guardrails
- Describe what exists; do not propose changes unless the user explicitly asks.
- Prefer source files over assumptions.
- Prefer concrete references over broad statements.
- Treat `AGENTS.md` as the durable project memory for this repo.
