# Repository Context

This is a multi-service repository.

## Path Awareness

Before working with files:

- Run `git rev-parse --show-toplevel` to identify the repository root.
- Treat the current working directory as the active workspace.
- Do not assume the working directory is the Git root.
- Resolve Git/patch paths relative to the repository root.
- Resolve local build/file commands relative to the current workspace.

## Scope

Only inspect or modify files relevant to the current task.
Do not modify sibling services unless explicitly required.