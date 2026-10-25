# AGENTS.md

Canonical guide for humans and coding agents working on **fhg-kit**. `CLAUDE.md` and any other agent entry file defer
to this document. Read it before changing anything.

## What this is

`fhg-kit` is a **shared Java library**, not an application. It packages cross-cutting building blocks that several
independent products need (LLM usage accounting and cost estimation first; credentials, feature flags, audit/consent,
integration contracts later) as small, framework-free modules with a single Spring Boot starter on top.

It contains **no business logic, no secrets, no infrastructure assumptions**. Anything environment-specific is a value
the consuming application provides.

## Project structure

| Module | Role |
|---|---|
| `fhg-kit-bom` | Bill of materials (Gradle `java-platform`); consumers import it and omit versions |
| `fhg-kit-llm` | AI usage event, recorders, price table, cost estimator, call tracker — plain Java 21 |
| `fhg-kit-spring-boot-starter` | Auto-configuration for the modules above. **The only module that knows Spring** (Boot 4.x) |

Each core module lives in `io.github.fhgomes.fhgkit.<module>` and is `@NullMarked` (jspecify) at package level.

## Build, test, release

| Action | Command |
|---|---|
| Build + test everything | `./gradlew build` |
| Try locally in an app | `./gradlew publishToMavenLocal` then `mavenLocal()` scoped to `io.github.fhgomes` in the app |
| Release | bump `libVersion` in `gradle.properties`, move `[Unreleased]` in `CHANGELOG.md`, commit, `git tag v<libVersion>`, push the tag; `publish.yml` publishes to GitHub Packages |

Build as a normal user, never with `sudo`. Tests run with JUnit 6 + AssertJ; there is no database, no container, no
network in the test suite and there must never be.

## Conventions (Do-Not-Break)

1. **Core modules are framework-free.** Java 21 standard library + `org.jspecify` + `slf4j-api` only. No Spring,
   Jackson, Lombok, JPA, Micrometer or HTTP client in a core module. Spring wiring goes in the starter, and only there.
2. **Public API = records, interfaces and small final classes.** Every public type has Javadoc. Validation happens in
   canonical constructors; collections are defensively copied and unmodifiable.
3. **Ports, not implementations.** Anything an application must plug in (persistence, metrics, transport) is an
   interface here. In-memory/logging defaults exist so the library works out of the box, but nothing persists on its own.
4. **Accounting never breaks the call.** Recorders and estimators are wrapped so a failure is logged and swallowed; the
   model call's result or exception always reaches the caller unchanged.
5. **No content in telemetry.** Events carry ids, counts, durations, model ids and short attribute values. Never a
   prompt, a completion, an error message body or anything that could contain personal data.
6. **No secrets, hosts or private names anywhere.** Not in code, tests, docs, examples or commit messages. Example
   values are obviously fake. Run a secrets/data-exposure review before every release.
7. **SemVer, and the CHANGELOG is the contract.** A breaking change to a public type is a major bump with a migration
   note. Deprecate first when possible.
8. **A module enters the kit only after the same need exists in at least two applications**, and it is extracted from
   the best existing implementation with its tests, not rewritten from scratch.
9. **Pinned versions.** Library and test dependency versions live in `gradle.properties`; the starter aligns to the
   Spring Boot BOM declared there. Floating versions are forbidden.
10. **Credentials for publishing come from the environment only** (`GITHUB_ACTOR`/`GITHUB_TOKEN` or `gpr.*` in the
    user's own `~/.gradle/gradle.properties`). Never in the repository.

## Constellation conventions (how this library fits the products)

The products that consume this kit are independent services ("stars") that integrate only through contracts:
REST (`/api/integration/v1`, RFC 7807 errors, `ETag`, preview → confirm), MCP with parity to the REST surface,
signed and idempotent outbound webhooks, and a shared AI usage event. `fhg-kit` implements the pieces of those
contracts that are pure library code; it never hosts a service, a database or an endpoint.

## Speckit

`.specify/` and `.claude/skills/speckit-*` are available for feature work (`/speckit-specify` → `/speckit-plan` →
`/speckit-tasks` → `/speckit-implement`). Keep specs small: one module or one port per feature.

## Further reading

- `README.md` — modules, consumption snippet, usage in 30 seconds
- `CHANGELOG.md` — what changed and what is unreleased
- `.ai/` — architecture notes, guidelines and testing conventions
