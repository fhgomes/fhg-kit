---
description: Quick implementation for simple tasks - minimal overhead
handoffs:
  - label: Full Workflow
    agent: speckit.specify
    prompt: For complex features, use /speckit-specify
---

## User Input

```text
$ARGUMENTS
```

## What This Is

Fast-track for simple tasks: bug fixes, small enhancements, config changes, minor refactoring.

**If task seems complex** → recommend full workflow (`/speckit-specify`).

## Execution

1. **Create feature directory**:
   ```bash
   .specify/scripts/bash/create-new-feature.sh --json "$ARGUMENTS" "Task description"
   ```

   Then persist the feature directory (the directory containing SPEC_FILE, relative to the repo root, e.g. `specs/012-my-task`) to `.specify/feature.json` as `{"feature_directory": "specs/012-my-task"}` — Spec Kit >= 1.0 uses this file (not the git branch) to locate the feature for `/speckit-plan`, `/speckit-tasks` and `/speckit-implement`.

2. **Write minimal spec.md**: Overview, requirements, success criteria. Skip everything else.

3. **Write minimal plan.md**: Files to change, approach, quick constitution check.

4. **Write minimal tasks.md**: 3-10 tasks max. Format: `- [ ] T001 Description`

5. **Implement**: Execute tasks, mark complete, report changes.

## If Unsure

1. Check `.specify/memory/` for context and guidelines
2. Check `.specify/templates/` for structure examples
3. Ask user for clarification

## Output

```markdown
## Done

**Feature**: [name]

### Changes:
- [file]: [what changed]

### Next Steps (you handle git):
- Review and test
- Branch/commit/push when ready
```

## Stop If

- Requirements unclear
- Architecture impact
- Security concerns
- Research needed

→ Tell user to use full workflow instead.