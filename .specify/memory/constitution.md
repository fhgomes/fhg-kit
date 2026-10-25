# Project Constitution

> Living governance document. Version: 0.1.0

## Core Principles

1. **Readability > Cleverness** — Code is read 10x more than written
2. **Explicit > Implicit** — No magic, no hidden behavior
3. **Simple > Complex** — Three similar lines before abstracting
4. **Maintainable > Fast** — Only optimize when measured
5. **Convention > Configuration** — Follow what the project already does

## Architecture Rules

- Layered architecture: Controller → Service → Repository
- Constructor injection only (never @Autowired on fields)
- DTOs for external boundaries, VOs for internal transport
- Database queries over in-memory filtering
- Flyway migrations: additive preferred, backward-compatible

## Code Standards

- Code in English, PRs in Portuguese, business docs in Portuguese
- No magic numbers, no wildcard imports, max 120 chars
- Early returns (guard clauses)
- Every code change must have a corresponding test change

## Quality Gates

Every task must pass BEFORE closing:
1. Build succeeds (no compilation errors)
2. Files exist (actually created)
3. Behavior works (endpoint responds, feature functions)
4. No regressions (didn't break existing functionality)
5. Follows project patterns (consistent with existing code)

## Versioning

- Semantic Versioning (MAJOR.MINOR.PATCH)
- Feature Branch Workflow
- All versions centralized in gradle.properties (Java) or package.json (Node)
