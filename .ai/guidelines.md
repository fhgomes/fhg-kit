# Project Guidelines — fhg-kit

> Git workflow, code review standards, and autonomy rules for AI agents.

## 1. Git Workflow

### Branch Strategy: Feature Branch Workflow
All work happens on feature branches. `main` is always deployable.

### Branch Naming
```
<type>/<description>
```

| Type | When |
|------|------|
| `feature/` | New feature or capability |
| `bugfix/` | Bug fix |
| `hotfix/` | Urgent production fix (branches from `main`, merges directly back) |
| `chore/` | Tooling, config, dependencies, CI/CD |
| `refactor/` | Code restructuring without behavior change |
| `docs/` | Documentation only |

**Examples:**
```
feature/order-checkout-flow
bugfix/fix-null-pointer-in-payment
hotfix/patch-auth-bypass
chore/upgrade-spring-boot-3.4
refactor/extract-notification-service
```

### Commit Messages
```
<type>: <description>
```

| Type | Meaning |
|------|---------|
| `feat` | New feature |
| `fix` | Bug fix |
| `chore` | Maintenance, dependencies |
| `refactor` | Code change with no behavior change |
| `test` | Adding or updating tests |
| `docs` | Documentation |
| `style` | Formatting, whitespace (no logic change) |
| `perf` | Performance improvement |
| `ci` | CI/CD changes |

**Rules:**
- First line: max 72 characters, imperative mood ("add", not "added" or "adds").
- Body (optional): explain **why**, not what. The diff shows what.
- Reference issue/ticket when applicable.

**Examples:**
```
feat: add email notification on order completion
fix: prevent duplicate payments on retry
refactor: extract payment validation to dedicated service
chore: bump Spring Boot to 3.4.1
test: add integration tests for checkout flow
```

## 2. Versioning

**Semantic Versioning:** `MAJOR.MINOR.PATCH`

| Component | When to bump |
|-----------|-------------|
| `MAJOR` | Breaking API changes |
| `MINOR` | New features, backward-compatible |
| `PATCH` | Bug fixes, backward-compatible |

## 3. Pull Request Standards

### PR Title
Same format as commit messages: `<type>: <description>`

### PR Description
Every PR MUST include:
1. **What:** Brief description of the change.
2. **Why:** Business context or issue reference.
3. **How:** Technical approach (for non-trivial changes).
4. **Testing:** What was tested and how.

### PR Checklist
Before submitting:
- [ ] All tests pass (`./gradlew test` or equivalent)
- [ ] No regressions introduced
- [ ] Follows coding standards (this document)
- [ ] Database migrations are backward-compatible
- [ ] No secrets or credentials in code
- [ ] API changes are documented
- [ ] New code has corresponding tests

## 4. Code Review Checklist

Reviewers should check:

### Correctness
- Does the code do what the PR says it does?
- Are edge cases handled?
- Is error handling appropriate?

### Naming & Readability
- Are class/method/variable names descriptive and consistent?
- Do class suffixes follow convention (Controller, Service, Repository, DTO, VO)?
- Is the code self-documenting? Would a new team member understand it?

### Patterns & Architecture
- Does it follow the project's established patterns?
- Is business logic in the service layer (not controllers or repositories)?
- Are dependencies injected via constructor?
- Is `@Transactional` used correctly (service layer, readOnly where appropriate)?

### Performance
- No N+1 queries?
- No `findAll().stream().filter()`?
- Pagination used for unbounded queries?
- Indexes exist for new foreign keys and query columns?

### Security
- No hardcoded secrets or credentials?
- Input validation present?
- Authorization checks in place?
- SQL injection prevention (parameterized queries)?

### Tests
- **Every code change must have a corresponding test change.**
- Unit tests for business logic.
- Integration tests for API endpoints and database queries.
- Edge cases and error paths tested.

## 5. AI Agent Autonomy Rules

These rules apply to ALL AI agents working on this project:

### DO — Just proceed:
- Create, edit, and delete files as needed.
- Run builds, tests, and linters.
- Create branches, commits, and PRs.
- Refactor code to follow these guidelines.
- Fix failing tests.
- Add missing tests for existing code.

### DO NOT — Stop and ask:
- Delete or significantly restructure entire modules.
- Change the database schema in production.
- Modify security configuration.
- Upgrade major framework versions (e.g., Spring Boot 3.x to 4.x).
- Change CI/CD pipeline configuration.

### Error Handling
- If a build fails, diagnose and fix it. Do not ask for permission.
- If a test fails, investigate the root cause. Fix the test or the code, whichever is wrong.
- If you encounter an ambiguous requirement, make a reasonable decision and document your assumption in the PR description.
- Only stop on **actual errors** that you cannot resolve, not on uncertainty about minor details.
