---
description: Unified spec-driven development workflow - from feature description to ready-to-implement task list
---

## User Input

```text
$ARGUMENTS
```

Consider the user input before proceeding (if not empty). This is your feature description.

## Overview

This command provides an **end-to-end spec-driven workflow** that takes a feature description and produces implementation-ready artifacts:

1. **Specification** — Define what to build (user stories, requirements, success criteria)
2. **Technical Plan** — Design how to build it (architecture, data model, contracts)
3. **Task Breakdown** — Create actionable implementation tasks

After completion, use `/speckit-implement` to execute the generated tasks.

**Use this for**:
- Simple to medium features with clear requirements
- Features that don't require extensive research
- Enhancements to existing functionality
- Well-understood new features

**Do NOT use this for**:
- Complex features requiring extensive research or multiple design iterations
- Features with unclear or ambiguous requirements
- Major architectural changes
- Integrations with unfamiliar external systems

---

## Phase 1: Specification

### 1.1 Setup Feature Directory

Generate a **short name** (2-4 words, kebab-case) from the feature description.

Determine the **feature number** by checking:
- Remote branches matching pattern `NNN-*`
- Local branches matching pattern `NNN-*`
- Existing directories in `specs/`

Use the next available number (N+1). If none exist, start with `001`.

Run the feature creation script:
```bash
.specify/scripts/bash/create-new-feature.sh --json "$ARGUMENTS" --number N+1 --short-name "your-short-name"
```

Then persist the feature directory (the directory containing SPEC_FILE, relative to the repo root, e.g. `specs/012-my-task`) to `.specify/feature.json` as `{"feature_directory": "specs/012-my-task"}` — Spec Kit >= 1.0 uses this file (not the git branch) to locate the feature for `/speckit-plan`, `/speckit-tasks` and `/speckit-implement`.

This creates:
- Directory: `specs/[NNN]-[short-name]/`

### 1.2 Write Specification

Create `specs/[NNN]-[short-name]/spec.md` using the template at `.specify/templates/spec-template.md`.

The specification must include:

**Feature Overview**
- Feature name and one-line description
- Problem statement (what pain point this solves)
- Business value (why this matters)

**User Stories**
For each distinct user workflow:
```
As a [user type],
I want to [action],
So that [benefit].

Acceptance Criteria:
- [ ] [Testable criterion 1]
- [ ] [Testable criterion 2]
```

**Functional Requirements**
Use RFC-style language (MUST, SHOULD, MAY):
- `FR-001`: System MUST [requirement]
- `FR-002`: System SHOULD [requirement]

Mark unclear items with `[NEEDS CLARIFICATION: reason]`

**Domain Model** (technology-agnostic)
- Key entities and their relationships
- Core attributes (without implementation details)

**Success Criteria** (measurable)
- `SC-001`: [Measurable metric, e.g., "Users complete task in under 2 minutes"]
- `SC-002`: [Business metric, e.g., "Reduce support tickets by 50%"]

**Scope**
- In Scope: What this feature includes
- Out of Scope: What this feature explicitly excludes
- Assumptions: What we assume to be true
- Dependencies: External dependencies or prerequisites

**Edge Cases & Error Handling**
- Edge case scenarios and expected behavior
- Error conditions and recovery strategies

### 1.3 Validate Specification

Create validation checklist at `specs/[NNN]-[short-name]/checklists/requirements.md`:

```markdown
# Specification Quality Checklist: [FEATURE NAME]

**Created**: [DATE]
**Feature**: [Link to spec.md]

## Content Quality
- [ ] No implementation details (languages, frameworks, APIs)
- [ ] Focused on user value and business needs
- [ ] Written for non-technical stakeholders
- [ ] All mandatory sections completed

## Requirement Completeness
- [ ] No [NEEDS CLARIFICATION] markers remain
- [ ] Requirements are testable and unambiguous
- [ ] Success criteria are measurable
- [ ] Edge cases identified

## Feature Readiness
- [ ] User stories have acceptance criteria
- [ ] Domain model captures key entities
- [ ] Scope boundaries are clear
- [ ] Dependencies identified
```

**Validation rules**:
- If `[NEEDS CLARIFICATION]` markers remain → Ask user for clarification (max 3 rounds)
- If requirements are ambiguous → Refine before proceeding
- All checklist items must pass before moving to Phase 2

### 1.4 Constitution Compliance

If `.specify/memory/constitution.md` exists:
- Read constitutional principles
- Document which principles apply to this feature
- Note any potential conflicts or considerations

---

## Phase 2: Technical Plan

### 2.1 Prerequisites Check

Verify before proceeding:
- `spec.md` exists and is complete
- All validation checks passed
- No unresolved `[NEEDS CLARIFICATION]` markers

### 2.2 Generate Technical Plan

Create `specs/[NNN]-[short-name]/plan.md` using `.specify/templates/plan-template.md`.

**Technical Context**
- Tech stack decisions (based on user input or project defaults)
- Framework versions and dependencies
- Infrastructure requirements

**Architecture Overview**
- High-level component diagram (described, not drawn)
- Component responsibilities
- Communication patterns

**Project Structure**
Choose appropriate structure:

```
# Option 1: Single project (CLI, library)
src/
├── models/
├── services/
├── cli/
└── lib/
tests/

# Option 2: Web application (frontend + backend)
backend/
├── src/
│   ├── models/
│   ├── services/
│   └── api/
└── tests/
frontend/
├── src/
│   ├── components/
│   ├── pages/
│   └── services/
└── tests/
```

**Data Model**
Create `specs/[NNN]-[short-name]/data-model.md`:
- Entity definitions with types
- Relationships and cardinality
- Database schema (if applicable)

**API Contracts** (if applicable)
Create `specs/[NNN]-[short-name]/contracts/`:
- `api-spec.json` — OpenAPI/REST endpoints
- Additional contract files as needed

**Implementation Approach**
- Development phases
- Key technical decisions with rationale
- Integration points

**Risk Assessment**
- Technical risks and mitigations
- Dependencies on external systems
- Performance considerations

### 2.3 Constitutional Validation

If constitution exists:
- Verify design decisions align with principles
- Document any trade-offs or exceptions
- **STOP** if design violates constitutional principles

---

## Phase 3: Task Breakdown

### 3.1 Prerequisites Check

Verify before proceeding:
- `plan.md` exists and is complete
- `spec.md` is accessible for user story reference
- Data model and contracts are defined (if applicable)

### 3.2 Generate Task List

Create `specs/[NNN]-[short-name]/tasks.md` using `.specify/templates/tasks-template.md`.

**Task Organization**

Tasks are organized by implementation phase:

```markdown
# Implementation Tasks: [FEATURE NAME]

**Branch**: [NNN]-[short-name]
**Spec**: [link to spec.md]
**Plan**: [link to plan.md]

## Legend
- [P] = Can run in parallel (different files, no dependencies)
- [US-N] = Belongs to User Story N

---

## Phase 1: Foundation (Required First)

### Project Setup
- [ ] [P] Create project structure and configuration files
- [ ] [P] Set up dependencies and package management
- [ ] Initialize database/storage (if applicable)

### Core Models
- [ ] [US-1,2,3] Create [Entity1] model with [attributes]
- [ ] [US-1,2] Create [Entity2] model with [attributes]

---

## Phase 2: Core Implementation

### User Story 1: [Story Title]
- [ ] [US-1] Implement [component/service]
- [ ] [US-1] Create [endpoint/handler]
- [ ] [US-1] Add [integration/connection]

### User Story 2: [Story Title]
- [ ] [P][US-2] Implement [component/service]
- [ ] [US-2] Create [endpoint/handler]

---

## Phase 3: Integration & Polish

- [ ] Wire up components
- [ ] Add error handling
- [ ] Implement edge cases from spec

---

## Checkpoints

After each phase, verify:
- [ ] Phase 1: Project runs, models defined
- [ ] Phase 2: Core features work independently
- [ ] Phase 3: Full integration complete
```

**Task Requirements**

Each task must:
- Be specific and actionable (one clear outcome)
- Include file paths where implementation occurs
- Reference the user story it fulfills `[US-N]`
- Mark parallel-safe tasks with `[P]`
- Respect dependencies (order matters)

**Dependency Rules**
- Models before services
- Services before endpoints/handlers
- Backend before frontend (for API dependencies)
- Foundation before features

---

## Completion Criteria

Before considering this command complete:

- [ ] **Phase 1**: `spec.md` written and validated, all checklist items pass
- [ ] **Phase 2**: `plan.md` complete with architecture and data model
- [ ] **Phase 3**: `tasks.md` generated with dependency-ordered tasks

---

## Output Summary

After completion, report:

```markdown
## Feature Ready for Implementation: [feature-name]

**Branch**: [NNN]-[short-name]
**Directory**: specs/[NNN]-[short-name]/

### Artifacts Created

| File | Status | Description |
|------|--------|-------------|
| spec.md | ✅ | Feature specification with user stories |
| checklists/requirements.md | ✅ | Validation checklist (all passed) |
| plan.md | ✅ | Technical design and architecture |
| data-model.md | ✅ | Entity definitions and relationships |
| contracts/ | ✅ | API specifications (if applicable) |
| tasks.md | ✅ | [N] implementation tasks, dependency-ordered |

### Summary
- User Stories: [N]
- Functional Requirements: [N]
- Implementation Tasks: [N] ([M] parallelizable)

### Next Steps
1. Review generated artifacts
2. Run `/speckit-implement` to execute tasks
3. Handle git operations (commits, push) yourself
```

---

## When to Stop

If you encounter any of these situations, **STOP** and inform the user:

**During Specification**:
- Requirements are too vague after 3 clarification attempts
- Feature scope is too large (recommend splitting)
- Constitutional violations detected

**During Planning**:
- Tech stack is unfamiliar (recommend research first)
- Multiple competing architectures need evaluation
- Significant unknowns require prototyping

**During Task Generation**:
- Plan has gaps that prevent task creation
- Dependencies cannot be resolved
- Scope creep detected (tasks don't match spec)

---

## Important Notes

- **No implementation**: This command only creates artifacts. Use `/speckit-implement` to execute.
- **No git operations**: User handles all branching, commits, and pushes.
- **Constitution first**: Always check constitutional compliance if `constitution.md` exists.
- **Quality over speed**: Don't skip validation to move faster.
- **Clarify early**: Ask questions during Phase 1, not during implementation.