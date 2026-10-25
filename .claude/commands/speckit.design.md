---
description: Generate both implementation plan (plan.md) and actionable tasks (tasks.md) from specification in a single workflow.
handoffs:
  - label: Implement Tasks
    agent: speckit.implement
    prompt: Execute the implementation tasks
    send: true
  - label: Analyze Artifacts
    agent: speckit.analyze
    prompt: Analyze the generated plan and tasks for consistency
    send: false
---

## User Input

```text
$ARGUMENTS
```

You **MUST** consider the user input before proceeding (if not empty).

## Outline

This command combines the `/speckit-plan` and `/speckit-tasks` workflows into a single execution to streamline feature development. It generates both the technical design (plan.md) and the actionable task breakdown (tasks.md) from the feature specification.

**Workflow:**
1. Setup and load feature context
2. Generate implementation plan (plan.md) with constitutional validation
3. Automatically derive tasks (tasks.md) from plan phases
4. Validate consistency between plan and tasks
5. Report generated artifacts

## Execution Steps

### Step 1: Setup and Load Context

1. **Run setup script** to get feature paths:
   ```bash
   .specify/scripts/bash/setup-plan.sh --json
   ```
   Parse JSON output for: `FEATURE_SPEC`, `IMPL_PLAN`, `TASKS_FILE`, `SPECS_DIR`

2. **Load required documents**:
   - Read `FEATURE_SPEC` (spec.md) - feature requirements
   - Read `.specify/memory/constitution.md` - governance principles
   - Read `.specify/memory/project-overview.md` - architecture context
   - Read `.specify/templates/plan-template.md` - plan structure
   - Read `.specify/templates/tasks-template.md` - tasks structure

3. **Verify prerequisites**:
   - Feature spec must exist and be complete
   - All MANDATORY sections in spec.md filled
   - Success criteria defined and measurable

---

### Step 2: Generate Implementation Plan (plan.md)

Follow the plan-template.md structure to create a comprehensive technical design:

#### 2.1 Executive Summary
- Synthesize spec.md into 2-3 paragraph overview
- List key deliverables (backend services, frontend components, database changes)
- Estimate timeline and complexity (Low/Medium/High/Critical)

#### 2.2 Constitution Check
**CRITICAL:** Validate against ALL 9 principles:

For each principle (1-9):
- **Alignment:** How this design adheres to the principle (or mark N/A)
- **Evidence:** Specific design choices demonstrating compliance
- **Gate:** PASSED | FAILED | N/A

**If ANY principle shows FAILED:**
- Document justification for exception
- Propose mitigation strategy
- Flag for review before proceeding

**Overall Compliance:** ✅ PASSED | ⚠ WITH EXCEPTIONS | ❌ FAILED

#### 2.3 Technical Architecture
- Draw ASCII component diagram showing three-layer architecture:
  - User Browser → Controllers → Business Logic → Selenium Services → Travian
- Document request flow for primary user action (step-by-step through layers)

#### 2.4 Service Decomposition
- **Business Logic Services:** List each service, responsibility, estimated line count (<500)
- **Selenium Services (if applicable):** List automation services, domains, line count (<500)
- **Frontend Modules:** List JavaScript modules, responsibilities, line count (<400)

#### 2.5 Implementation Phases
Break implementation into 5-9 phases:
- **Phase 1:** Backend - Business Logic Layer
- **Phase 2:** Backend - Selenium Services (if applicable)
- **Phase 3:** Backend - Data Persistence (if applicable)
- **Phase 4:** Frontend - HTML/CSS/JavaScript
- **Phase 5:** Configuration & Infrastructure
- **Phase 6:** Security & Observability
- **Phase 7:** Testing
- **Phase 8:** Documentation
- **Phase 9:** Deployment & Verification

For each phase:
- **Tasks:** 3-7 high-level task descriptions
- **Deliverables:** Specific files/components created
- **Validation:** Testable checkpoints (checkboxes)

#### 2.6 Remaining Sections
Fill in remaining plan template sections:
- Data Model Implementation (SQL schemas, Java records/entities, DTOs)
- API Implementation (new/updated endpoints table)
- Security Implementation (if handling sensitive data)
- Performance Targets (measurable metrics table)
- Testing Strategy (test pyramid, coverage breakdown)
- Deployment Strategy (local, Docker, environment variables)
- Risk Assessment (risks, probability, impact, mitigation)
- Success Criteria Verification (map spec criteria to tests)
- Migration Path (if applicable)
- Open Questions / Deferred Decisions

#### 2.7 Write plan.md
Write the complete plan to `IMPL_PLAN` path from setup script.

---

### Step 3: Generate Actionable Tasks (tasks.md)

**Automatically derive tasks from plan.md phases** using tasks-template.md structure:

#### 3.1 Task Numbering
- Sequential numbering: T001, T002, T003, etc.
- Organize by implementation phases from plan.md

#### 3.2 Task Generation Logic

For each phase in plan.md:
1. **Extract high-level tasks** from phase description
2. **Break down into specific coding tasks** (create file, implement method, write test)
3. **Tag with constitutional principle** ([SIMPLE], [DOCKER], [JAVA21], [DATA], [SELENIUM], [OBSERV], [SECURITY], [COMPAT], [MODULAR])
4. **Add implementation details** (file paths, class names, method signatures)
5. **Set initial status** (all tasks start as `[ ]` Not Started)

#### 3.3 Task Categories

**Phase 1: Backend - Business Logic Layer**
- Controllers: Create REST endpoint classes
- Business Logic Services: Create service classes with single responsibility
- DTOs: Create request/response records with validation

**Phase 2: Backend - Selenium Services (if applicable)**
- Selenium automation: Create Selenium*Service classes
- Explicit waits: Implement WebDriverWait for DOM interactions
- Retry logic: Add exponential backoff
- Screenshot capture: Implement error screenshots

**Phase 3: Backend - Data Persistence (if applicable)**
- Database schema: Create migration scripts
- JPA entities: Create @Entity classes
- Repositories: Create JpaRepository interfaces

**Phase 4: Frontend - HTML/CSS/JavaScript**
- HTML views: Create .html files for user workflows
- CSS stylesheets: Create modular .css files (<300 lines)
- JavaScript modules: Create .js files with single responsibility (<400 lines)

**Phase 5: Configuration & Infrastructure**
- Application config: Update application.properties
- Docker: Update Dockerfile, docker-compose.yml
- Environment variables: Document new config

**Phase 6: Security & Observability**
- Security: Implement encryption, logging redaction, auth updates
- Logging: Add structured logging with context
- Action tracking: Log to ActionsService
- Metrics: Expose Actuator metrics

**Phase 7: Testing**
- Unit tests: Test services, DTOs (>80% coverage)
- Integration tests: Test endpoints, database operations
- Selenium tests: Test automation services
- E2E tests (optional): Test full user workflows

**Phase 8: Documentation**
- Code documentation: Add JavaDoc, inline comments
- User documentation: Update README, quickstart
- Architecture docs: Update diagrams

**Phase 9: Deployment & Verification**
- Pre-deployment: Run tests, build Docker, security audit
- Post-deployment: Verify in production-like environment, UAT

#### 3.4 Write tasks.md
Write the complete task list to `TASKS_FILE` path from setup script.

---

### Step 4: Validate Consistency

Perform cross-document validation:

1. **Plan ↔ Tasks Consistency:**
   - Every phase in plan.md has corresponding tasks in tasks.md
   - Task counts align with phase complexity
   - No orphaned tasks (tasks not mapped to a phase)

2. **Spec ↔ Plan Consistency:**
   - All functional requirements covered in plan
   - Success criteria have verification methods in plan
   - Dependencies acknowledged in plan

3. **Constitution Compliance:**
   - Plan passed constitutional validation (or exceptions documented)
   - Tasks tagged with relevant principles
   - Service line count limits enforced in tasks

**If inconsistencies found:**
- List specific issues
- Suggest corrections
- Update plan.md or tasks.md as needed

---

### Step 5: Report Generated Artifacts

**Output final summary:**

```
✅ Design Complete: [FEATURE_NAME]

Generated Artifacts:
- 📋 plan.md: [PATH_TO_PLAN]
- ✅ tasks.md: [PATH_TO_TASKS]

Constitutional Compliance: [PASSED/WITH EXCEPTIONS/FAILED]
Implementation Phases: [N phases]
Total Tasks: [X tasks]

Estimated Implementation Time: [Y days/weeks]

Next Steps:
1. Review plan.md for architectural soundness
2. Review tasks.md for completeness
3. Create git branch if needed (user handles branching)
4. Run /speckit-implement to execute tasks (or implement manually)
5. Commit and push when ready (user handles git operations)
```

---

## Key Differences from Individual Commands

### Advantages of /speckit.design:
1. **Single execution** - No need to run /speckit-plan then /speckit-tasks
2. **Automatic consistency** - Tasks derived directly from plan phases
3. **Faster workflow** - Saves time by generating both in one pass
4. **Guaranteed alignment** - Plan and tasks created together, no drift

### When to use /speckit-plan + /speckit-tasks separately:
- You want to **review the plan** before generating tasks
- You need to **modify the plan** before task breakdown
- You prefer **explicit control** over each workflow step

### When to use /speckit.design:
- You want **rapid prototyping** of design + tasks
- You trust the plan generation and want tasks immediately
- You prefer **streamlined workflow** with fewer steps
- You're implementing a **well-understood feature** with clear requirements

---

## Error Handling

**If spec.md missing or incomplete:**
- STOP execution
- Report missing sections
- Suggest running `/speckit-specify` first

**If constitutional violations detected:**
- Mark Overall Compliance as FAILED
- List specific violations
- Require explicit user acknowledgment to proceed

**If task generation fails:**
- Write plan.md successfully
- Report task generation error
- Suggest running `/speckit-tasks` manually

---

## Template References

- Plan structure: `.specify/templates/plan-template.md`
- Tasks structure: `.specify/templates/tasks-template.md`
- Spec structure: `.specify/templates/spec-template.md`
- Constitution: `.specify/memory/constitution.md`
- Project overview: `.specify/memory/project-overview.md`

---

**Command Version:** 1.0.0
**Last Updated:** 2026-01-24
**Dependencies:** speckit.specify (must run first)
