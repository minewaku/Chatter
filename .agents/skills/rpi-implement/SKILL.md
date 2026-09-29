---
name: rpi-implement
description: Use when the user wants to implement an approved plan in small verified checkpoints with compaction between steps.
---

# RPI Implement

## Purpose
Execute an approved plan in small, reviewable batches.

## Use This Skill When
- The plan is approved and code changes should begin.
- The user wants incremental implementation with validation.
- The work should stay narrow and checkpoint-based.

## Workflow
1. Restate the active goal and constraints from the plan.
2. Read the plan and referenced files fully.
3. Make the smallest useful code change for the current step.
4. Verify the change before moving on.
5. Return to the source plan and mark completed checklist items before moving to the next phase.
6. Repeat until the planned scope is complete.
7. **When all steps are done, update the plan checklist to mark every item as completed before yielding to the user.**

## Output Shape
- What changed
- Files touched
- Validation performed
- Remaining work or follow-ups

## Token Budget
- The session token limit is **400,000 tokens**.
- Track usage continuously. At **~45% (~180,000 tokens used)**, treat it as a compaction trigger.
- **Never interrupt an in-progress response or tool call to compact.** Always wait for the current output, file write, or command to finish completely before triggering compaction.

## Compaction Rule
Compaction is mandatory.
- **Compact when context reaches 45% of the 400K token budget (~180K tokens used)** — do not wait for a checkpoint boundary.
- Also compact after each implementation checkpoint, after any validation run, and before moving to the next file group.
- If the compaction threshold is hit mid-response or mid-tool-call, finish the current output fully first, then compact immediately after.
- Before compacting, write a savepoint file to `docs/savepoints/` in the project root:
  - Filename: `savepoints/<YYYY-MM-DD_HH-MM>_<slug>.md` where slug is a 2–3 word kebab-case description of the current step.
  - Contents:
    - Goal
    - Current step
    - Files changed (with brief note on what changed)
    - Validation status
    - Remaining steps
    - Open issues
- After writing the savepoint, compact the conversation to only the savepoint contents.
- Inform the user: "Savepoint written. Starting a new session — I'll pick up from the savepoint automatically."

## Resume Rule
When the user says "pick up where we left off" or equivalent:
1. Read the most recent file in `docs/savepoints/` (sort by filename descending to find latest).
2. Restate the goal, current step, and remaining steps from the savepoint so the user can confirm context is correct.
3. Continue executing the plan checklist from the next uncompleted step — do not re-do completed steps.

## Completion Rule
When all plan steps are done:
1. Update every checklist item in the plan file to `[x]` (checked).
2. Write a final savepoint to `docs/savepoints/`.
3. Report what was completed, files touched, and validation status.
4. Ask the user what's next.

## Guardrails
- Prefer existing patterns in the repo.
- Do not widen scope midstream without explicit user approval.
- Verify with the most relevant build or test command after each meaningful batch.
- Keep the plan synchronized with implementation progress at all times.
