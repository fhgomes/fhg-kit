---
description: Simplified workflow for simple tasks - combines specify, plan, tasks, and implement in one command
handoffs:
  - label: Full Workflow
    agent: speckit.specify
    prompt: For complex features, use the full workflow starting with /speckit-specify
---

## User Input

```text
$ARGUMENTS
```

You **MUST** consider the user input before proceeding (if not empty).

## Overview

This is a **simplified, streamlined workflow** for simple tasks that don't require extensive planning, research, or validation. It combines all phases (specify → plan → tasks → implement) into one command.

**Use this for**:
- Bug fixes
- Small enhancements to existing features
- Simple CRUD operations
- Minor refactoring tasks
- Configuration changes

**DO NOT use this for**:
- New major features
- Complex integrations
- Architecture changes
- Tasks requiring research or design decisions
- Features with unclear requirements

## Execution Flow

### Step 1: Feature Directory Setup

1. **Generate a concise short name** (2-4 words) from the task description:
    - Use action-noun format: "fix-login-bug", "add-user-field", "update-config"
    - Keep it simple and descriptive

2. **Find next feature number**:
    - Check existing `specs/[0-9]+-*` directories
    - Use N+1 for the new feature

3. **Create feature structure**:
   ```bash
   .specify/scripts/bash/create-new-feature.sh --json "$ARGUMENTS" --number N+1 --short-name "your-short-name" "Task description"
   ```

   Then persist the feature directory (the directory containing SPEC_FILE, relative to the repo root, e.g. `specs/012-my-task`) to `.specify/feature.json` as `{"feature_directory": "specs/012-my-task"}` — Spec Kit >= 1.0 uses this file (not the git branch) to locate the feature for `/speckit-plan`, `/speckit-tasks` and `/speckit-implement`.

   Parse JSON output for SPEC_FILE, FEATURE_DIR paths.

### Step 2: Quick Specification

Create a **minimal spec.md** (skip extensive validation):

1. Load `.specify/templates/spec-template.md`

2. Fill only **essential sections**:
    - **Feature Name**: Short, descriptive name
    - **Overview**: 1-2 sentences describing the task
    - **Functional Requirements**: What needs to be done (bullet points)
    - **Success Criteria**: How to verify it's done (2-3 items max)
    - **Assumptions**: Any assumptions made (if applicable)

3. **Skip these sections** for simple tasks:
    - User Scenarios & Testing (unless specifically needed)
    - Key Entities (unless data model changes)
    - Dependencies (unless obvious external deps)
    - Out of Scope (unless important to clarify)

4. **No validation needed**: Write spec directly, no checklist generation or clarification prompts

### Step 3: Quick Planning

Create a **minimal plan.md** (skip research and extensive design):

1. Load plan template structure

2. Fill only **core sections**:
    - **Technical Context**:
        - Tech stack (from existing project - just list what's needed)
        - File locations (where changes will be made)
        - Dependencies (libraries/packages if new ones needed)

    - **Constitution Check**: Run quick check against `.specify/memory/constitution.md`
        - Only ERROR if clear violations
        - Skip detailed analysis

    - **Implementation Approach**:
        - Brief description of how to implement (2-3 sentences)
        - List files to create/modify
        - Any special considerations

3. **Skip these artifacts**:
    - research.md (no unknowns for simple tasks)
    - data-model.md (unless data changes)
    - contracts/ (unless API changes)
    - quickstart.md (not needed for simple tasks)

4. **No phases**: Write the plan directly without Phase 0/1/2 separation

### Step 4: Task Generation

Generate a **simplified tasks.md**:

1. Load tasks template structure

2. Create **minimal task list**:
    - **Phase 1: Setup** (if needed):
        - Install dependencies (if any new packages)
        - Update configuration (if needed)

    - **Phase 2: Implementation**:
        - List specific file changes as individual tasks
        - Use checklist format: `- [ ] T001 [P] Description with file path`
        - Mark parallelizable tasks with [P]

    - **Phase 3: Validation** (optional):
        - Run tests (if tests exist for affected code)
        - Verify functionality

3. **Keep it simple**:
    - Total tasks: 3-10 tasks max for simple work
    - No user story organization (unless applicable)
    - No extensive dependency graphs
    - No parallel execution examples (just mark [P] where applicable)

### Step 5: Implementation

Execute tasks immediately:

1. **Verify ignore files** (quick check):
    - Ensure .gitignore exists and has basic patterns
    - Skip creating other ignore files unless explicitly needed

2. **Execute tasks sequentially**:
    - Follow task order from tasks.md
    - Mark tasks as [X] when completed
    - Report progress after each task

3. **Error handling**:
    - Stop on first error
    - Report what failed and why
    - Suggest fixes or next steps

4. **Completion**:
    - Mark all tasks complete in tasks.md
    - Report summary of changes
    - List modified files

## Guidelines

### Speed vs Quality Trade-offs

For simple tasks, prioritize speed:
- **Skip**: Extensive validation, quality checklists, clarification prompts
- **Skip**: Research phase, multiple design artifacts
- **Skip**: Complex dependency analysis
- **Skip**: Git commit/push/branch operations (user handles branching and commits)
- **Keep**: Constitution checks (security/privacy gates)
- **Keep**: Clear task breakdown
- **Keep**: Progress tracking

### When to Stop and Use Full Workflow

If you encounter ANY of these, **STOP** and recommend using full workflow:

1. **Unclear requirements**: Multiple interpretations possible
2. **Architecture impact**: Changes affect system design
3. **New integrations**: External services, APIs, databases
4. **Security concerns**: Authentication, authorization, data privacy
5. **Complex dependencies**: Multiple interconnected changes
6. **Research needed**: Technology choices, best practices unclear

In these cases, tell user:
```text
This task requires more extensive planning. Please use:
1. /speckit-specify - for detailed requirements
2. /speckit-plan - for technical design
3. /speckit-tasks - for task breakdown
4. /speckit-implement - for execution
```

### Task Checklist Format

**REQUIRED** - Every task must follow:
```text
- [ ] T001 [P] Description with exact file path
```

Components:
- `- [ ]`: Markdown checkbox
- `T001`: Sequential task ID
- `[P]`: Parallel marker (optional - only if truly parallelizable)
- Description: Clear action + file path

### Output Structure

After completion, report:

```markdown
## Simple Task Implementation Complete

**Feature**: [feature-name]
**Spec Directory**: specs/[N]-[short-name]/

### Changes Made:
- [file1]: [brief description]
- [file2]: [brief description]
- ...

### Tasks Completed: X/X

### Next Steps (user handles git):
- Review changes in your editor
- Test functionality manually
- Create branch if needed
- Commit and push when ready
```

## Important Notes

- **No git operations**: User handles all branching, commits, and pushes
- **Fast execution**: Aim for quick turnaround on simple tasks
- **Minimal documentation**: Only essential artifacts (spec, plan, tasks)
- **Constitution compliance**: Always check, but don't over-analyze
- **Error transparency**: Report issues clearly and suggest solutions
- **Scope awareness**: Detect when task is too complex and recommend full workflow

## Example Use Cases

Good for this command:
- "Add a new field 'phone_number' to User model"
- "Fix bug in login validation - allow emails without TLD"
- "Update API endpoint to return user's full name instead of just first name"
- "Add logging to payment processing function"
- "Change database connection timeout from 30s to 60s"

Should use full workflow instead:
- "Implement user authentication system"
- "Integrate with Stripe payment API"
- "Redesign database schema for better performance"
- "Add real-time notifications with WebSockets"
- "Implement caching layer for API responses"